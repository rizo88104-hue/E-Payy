package uz.epay.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import uz.epay.EPayApplication;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StartupValidatorTest {
    private static final String STRONG = "very-strong-admin-pw";

    private static EpayProperties props(String adminPassword, String publicUrl, String cors,
                                        String paymeKey, String paymeUrl) {
        return new EpayProperties(publicUrl, new EpayProperties.Cors(cors), null,
                new EpayProperties.Admin("admin", adminPassword), new EpayProperties.Payment(new BigDecimal("12650")),
                new EpayProperties.Payme(null, paymeKey, paymeUrl), null, null);
    }

    @Test
    void validConfigPasses() {
        assertThat(StartupValidator.validate(props(STRONG, "https://epay.uz", "https://epay.uz",
                "key", "https://checkout.paycom.uz"))).isEmpty();
    }

    @Test
    void weakOrMissingAdminPassword() {
        assertThat(StartupValidator.validate(props("short-pass", "https://epay.uz", "https://epay.uz", null, null)))
                .containsExactly("ADMIN_PASSWORD kamida 12 belgi bo'lishi kerak");
        assertThat(StartupValidator.validate(props(null, "https://epay.uz", "https://epay.uz", null, null)))
                .hasSize(1);
    }

    @Test
    void publicUrlAndCorsMustBeHttps() {
        assertThat(StartupValidator.validate(props(STRONG, null, "http://epay.uz", null, null)))
                .containsExactly("PUBLIC_URL (epay.public-url) berilmagan",
                        "CORS_ALLOWED_ORIGIN (epay.cors.allowed-origin) https:// bilan boshlanishi kerak: http://epay.uz");
    }

    @Test
    void paymentKeysWithSandboxUrl() {
        assertThat(StartupValidator.validate(props(STRONG, "https://epay.uz", "https://epay.uz",
                "key", "https://test.paycom.uz")))
                .containsExactly("Payme kaliti berilgan, lekin manzil sandbox: https://test.paycom.uz");
        // kalit yo'q — sandbox manzil muammo emas (to'lov hali ulanmagan)
        assertThat(StartupValidator.validate(props(STRONG, "https://epay.uz", "https://epay.uz",
                null, "https://test.paycom.uz"))).isEmpty();
    }

    @Test
    void usdRateIsRequired() {
        EpayProperties noRate = new EpayProperties("https://epay.uz", new EpayProperties.Cors("https://epay.uz"), null,
                new EpayProperties.Admin("admin", STRONG), null, null, null, null);
        assertThat(StartupValidator.validate(noRate))
                .containsExactly("PAYMENT_USD_RATE (epay.payment.usd-rate) berilmagan yoki musbat emas");
    }

    /** Haqiqiy prod konteksti: zaif parol bilan ilova ishga tushmaydi. */
    @Test
    void prodContextFailsWithWeakPassword() {
        Map<String, Object> p = prodProperties();
        p.put("epay.admin.password", "admin123");
        assertThatThrownBy(() -> start(p))
                .rootCause()
                .hasMessageContaining("Prod sozlamalari xato")
                .hasMessageContaining("ADMIN_PASSWORD kamida 12 belgi");
    }

    @Test
    void prodContextStartsWithValidConfig() {
        try (ConfigurableApplicationContext ctx = start(prodProperties())) {
            assertThat(ctx.getBean(StartupValidator.class)).isNotNull();
        }
    }

    private static Map<String, Object> prodProperties() {
        Map<String, Object> p = new HashMap<>();
        p.put("spring.datasource.url", "jdbc:h2:mem:prodcheck;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        p.put("spring.datasource.username", "sa");
        p.put("spring.datasource.password", "");
        p.put("server.port", "0");
        p.put("epay.admin.password", STRONG);
        p.put("epay.public-url", "https://epay.uz");
        p.put("epay.cors.allowed-origin", "https://epay.uz");
        p.put("epay.upload-dir", "target/prodcheck-uploads");
        p.put("epay.payment.usd-rate", "12650");
        return p;
    }

    /** Buyruq qatori argumentlari — application-prod.yml dagi ${SPRING_DATASOURCE_URL} dan ustun turadi. */
    private static ConfigurableApplicationContext start(Map<String, Object> properties) {
        String[] args = properties.entrySet().stream().map(e -> "--" + e.getKey() + "=" + e.getValue())
                .toArray(String[]::new);
        return new SpringApplicationBuilder(EPayApplication.class).profiles("prod").run(args);
    }
}
