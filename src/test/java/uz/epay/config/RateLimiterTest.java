package uz.epay.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uz.epay.config.RateLimiter.Limit;
import uz.epay.exception.TooManyRequestsException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterTest {
    private static final String IP = "10.0.0.1";

    private MutableClock clock;
    private RateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        // null -> standart limitlar: ORDER 5/10m, TRACK 10/1m, LOGIN 10/15m
        limiter = new RateLimiter(new RateLimitProperties(null, null, null), clock);
    }

    @Test
    void allowsUpToMaxThenRejects() {
        for (int i = 0; i < 5; i++) {
            limiter.check(Limit.ORDER, IP);
        }
        assertThatThrownBy(() -> limiter.check(Limit.ORDER, IP))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessage(RateLimiter.MESSAGE);
    }

    @Test
    void windowResetsAfterExpiry() {
        for (int i = 0; i < 5; i++) {
            limiter.check(Limit.ORDER, IP);
        }
        clock.advance(Duration.ofMinutes(10).minusMillis(1));
        assertThatThrownBy(() -> limiter.check(Limit.ORDER, IP)).isInstanceOf(TooManyRequestsException.class);

        clock.advance(Duration.ofMillis(1));
        assertThatCode(() -> limiter.check(Limit.ORDER, IP)).doesNotThrowAnyException();
    }

    @Test
    void retryAfterIsTimeLeftInWindow() {
        for (int i = 0; i < 5; i++) {
            limiter.check(Limit.ORDER, IP);
        }
        clock.advance(Duration.ofMinutes(3));
        assertThatThrownBy(() -> limiter.check(Limit.ORDER, IP))
                .isInstanceOfSatisfying(TooManyRequestsException.class,
                        e -> assertThat(e.getRetryAfter()).isEqualTo(Duration.ofMinutes(7)));
    }

    @Test
    void ipsAreCountedSeparately() {
        for (int i = 0; i < 5; i++) {
            limiter.check(Limit.ORDER, IP);
        }
        assertThatThrownBy(() -> limiter.check(Limit.ORDER, IP)).isInstanceOf(TooManyRequestsException.class);
        assertThatCode(() -> limiter.check(Limit.ORDER, "10.0.0.2")).doesNotThrowAnyException();
    }

    @Test
    void limitsAreCountedSeparately() {
        for (int i = 0; i < 5; i++) {
            limiter.check(Limit.ORDER, IP);
        }
        assertThatCode(() -> limiter.check(Limit.TRACK, IP)).doesNotThrowAnyException();
    }

    @Test
    void loginBlockedAfterMaxFailures() {
        for (int i = 0; i < 9; i++) {
            limiter.hit(Limit.LOGIN, IP);
        }
        assertThat(limiter.blocked(Limit.LOGIN, IP)).isFalse();
        assertThat(limiter.hit(Limit.LOGIN, IP)).isEqualTo(10);
        assertThat(limiter.blocked(Limit.LOGIN, IP)).isTrue();
        assertThat(limiter.blocked(Limit.LOGIN, "10.0.0.2")).isFalse();

        clock.advance(Duration.ofMinutes(15));
        assertThat(limiter.blocked(Limit.LOGIN, IP)).isFalse();
    }

    @Test
    void customLimitsFromProperties() {
        var props = new RateLimitProperties(new RateLimitProperties.Rule(1, Duration.ofSeconds(30)), null, null);
        var custom = new RateLimiter(props, clock);
        custom.check(Limit.ORDER, IP);
        assertThatThrownBy(() -> custom.check(Limit.ORDER, IP)).isInstanceOf(TooManyRequestsException.class);
        clock.advance(Duration.ofSeconds(30));
        assertThatCode(() -> custom.check(Limit.ORDER, IP)).doesNotThrowAnyException();
    }

    /** Qo'lda suriladigan soat. */
    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
