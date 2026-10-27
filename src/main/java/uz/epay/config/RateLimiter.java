package uz.epay.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uz.epay.exception.TooManyRequestsException;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimiter {
    static final String MESSAGE = "Juda ko'p urinish. Birozdan keyin qayta urinib ko'ring.";
    private static final int CLEANUP_THRESHOLD = 10_000;

    public enum Limit {
        ORDER,
        TRACK,
        LOGIN,
        LOGIN_USER
    }

    private record Window(long start, int count) {
    }

    private final Map<String, Window> hits = new ConcurrentHashMap<>();
    private final Map<Limit, RateLimitProperties.Rule> rules;
    private final Clock clock;

    @Autowired
    public RateLimiter(RateLimitProperties props) {
        this(props, Clock.systemUTC());
    }

    RateLimiter(RateLimitProperties props, Clock clock) {
        this.rules = Map.of(Limit.ORDER, props.order(), Limit.TRACK, props.track(), Limit.LOGIN, props.login(),
                Limit.LOGIN_USER, props.loginUser());
        this.clock = clock;
    }

    public void check(Limit limit, String ip) {
        RateLimitProperties.Rule rule = rules.get(limit);
        Window w = record(limit, ip);
        if (w.count() > rule.max()) {
            long left = rule.window().toMillis() - (clock.millis() - w.start());
            throw new TooManyRequestsException(MESSAGE, Duration.ofMillis(Math.max(left, 0)));
        }
    }

    public int hit(Limit limit, String ip) {
        return record(limit, ip).count();
    }

    public boolean blocked(Limit limit, String ip) {
        RateLimitProperties.Rule rule = rules.get(limit);
        Window w = hits.get(key(limit, ip));
        return w != null && clock.millis() - w.start() < rule.window().toMillis() && w.count() >= rule.max();
    }

    public Duration blockedFor(Limit limit, String key) {
        if (!blocked(limit, key)) {
            return Duration.ZERO;
        }
        Window w = hits.get(key(limit, key));
        return Duration.ofMillis(Math.max(0, rules.get(limit).window().toMillis() - (clock.millis() - w.start())));
    }

    public void reset(Limit limit, String key) {
        hits.remove(key(limit, key));
    }

    private Window record(Limit limit, String ip) {
        long now = clock.millis();
        long win = rules.get(limit).window().toMillis();
        Window w = hits.compute(key(limit, ip), (k, old) ->
                old == null || now - old.start() >= win ? new Window(now, 1) : new Window(old.start(), old.count() + 1));
        if (hits.size() > CLEANUP_THRESHOLD) {
            cleanup(now);
        }
        return w;
    }

    private void cleanup(long now) {
        hits.entrySet().removeIf(e -> {
            Limit limit = Limit.valueOf(e.getKey().substring(0, e.getKey().indexOf('|')));
            return now - e.getValue().start() >= rules.get(limit).window().toMillis();
        });
    }

    private static String key(Limit limit, String ip) {
        return limit + "|" + ip;
    }
}
