package uz.epay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Limitlar application.yml dagi epay.rate-limit.* dan; berilmasa — standart qiymatlar.
 * login — IP bo'yicha xato urinishlar; loginUser — bitta login nomiga barcha IP lardan (yumshoqroq,
 * begona odam xodimning akkauntini oson bloklab qo'ya olmasin).
 */
@ConfigurationProperties("epay.rate-limit")
public record RateLimitProperties(Rule order, Rule track, Rule login, Rule loginUser) {

    public RateLimitProperties {
        order = order != null ? order : new Rule(5, Duration.ofMinutes(10));
        track = track != null ? track : new Rule(10, Duration.ofMinutes(1));
        login = login != null ? login : new Rule(10, Duration.ofMinutes(15));
        loginUser = loginUser != null ? loginUser : new Rule(30, Duration.ofMinutes(15));
    }

    /** window ichida ko'pi bilan max urinish. */
    public record Rule(int max, Duration window) {
    }
}
