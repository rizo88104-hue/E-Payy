package uz.epay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;

@ConfigurationProperties("epay")
public record EpayProperties(String publicUrl, Cors cors, Security security, Admin admin, Payment payment,
                             Payme payme, Click click, OrderSettings order) {

    public EpayProperties {
        cors = cors != null ? cors : new Cors(null);
        security = security != null ? security : new Security(false);
        admin = admin != null ? admin : new Admin(null, null);
        payment = payment != null ? payment : new Payment(null);
        payme = payme != null ? payme : new Payme(null, null, null);
        click = click != null ? click : new Click(null, null, null);
        order = order != null ? order : new OrderSettings(null, null, 0);
    }

    public record Payment(BigDecimal usdRate) {
    }

    public record OrderSettings(Duration unpaidTimeout, Map<Integer, Integer> warranty, int maxPendingPerPhone) {
        public OrderSettings {
            unpaidTimeout = unpaidTimeout != null ? unpaidTimeout : Duration.ofHours(24);
            warranty = new TreeMap<>(warranty == null || warranty.isEmpty() ? Map.of(1, 0, 3, 10, 5, 20) : warranty);
            maxPendingPerPhone = maxPendingPerPhone > 0 ? maxPendingPerPhone : 3;
        }
    }

    public record Cors(String allowedOrigin) {
    }

    public record Security(boolean warehouseCanEditPrice) {
    }

    public record Admin(String username, String password) {
        public Admin {
            username = username == null || username.isBlank() ? "admin" : username.trim().toLowerCase();
        }
    }

    public record Payme(String merchantId, String key, String checkoutUrl) {
    }

    public record Click(String serviceId, String secretKey, String apiUrl) {
    }
}
