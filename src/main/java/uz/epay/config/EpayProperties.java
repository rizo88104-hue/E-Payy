package uz.epay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ilova sozlamalari (application.yml dagi epay.*). Kalitlar faqat muhit o'zgaruvchilaridan keladi.
 * epay.rate-limit — alohida (RateLimitProperties).
 */
@ConfigurationProperties("epay")
public record EpayProperties(String publicUrl, Cors cors, Security security, Admin admin, Payme payme, Click click) {

    public EpayProperties {
        cors = cors != null ? cors : new Cors(null);
        security = security != null ? security : new Security(false);
        admin = admin != null ? admin : new Admin(null, null);
        payme = payme != null ? payme : new Payme(null, null, null);
        click = click != null ? click : new Click(null, null, null);
    }

    /** allowedOrigin — yagona ruxsat etilgan domen (masalan https://epay.uz); bo'sh — CORS yo'q. Hech qachon "*". */
    public record Cors(String allowedOrigin) {
    }

    /** warehouseCanEditPrice — omborchi katalogda narxni o'zgartira oladimi (standart: yo'q). */
    public record Security(boolean warehouseCanEditPrice) {
    }

    /** Birinchi admin: xodimlar jadvali bo'sh bo'lsa yaratiladi. */
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
