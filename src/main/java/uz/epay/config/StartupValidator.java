package uz.epay.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * prod: xavfli sozlama bilan ilova ishga TUSHMAYDI — barcha xatolar bitta aniq xabarda.
 * Konstruktorda tekshiriladi, shuning uchun kontekst yuklanishi shu yerda to'xtaydi.
 */
@Component
@Profile("prod")
public class StartupValidator {
    static final int MIN_ADMIN_PASSWORD = 12;

    public StartupValidator(EpayProperties props) {
        List<String> errors = validate(props);
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Prod sozlamalari xato, ilova ishga tushmaydi:\n - "
                    + String.join("\n - ", errors));
        }
    }

    static List<String> validate(EpayProperties props) {
        List<String> errors = new ArrayList<>();

        String password = props.admin().password();
        if (password == null || password.length() < MIN_ADMIN_PASSWORD) {
            errors.add("ADMIN_PASSWORD kamida " + MIN_ADMIN_PASSWORD + " belgi bo'lishi kerak");
        }

        requireHttps(errors, "PUBLIC_URL (epay.public-url)", props.publicUrl());
        requireHttps(errors, "CORS_ALLOWED_ORIGIN (epay.cors.allowed-origin)", props.cors().allowedOrigin());

        if (isSet(props.payme().key()) && isSandbox(props.payme().checkoutUrl())) {
            errors.add("Payme kaliti berilgan, lekin manzil sandbox: " + props.payme().checkoutUrl());
        }
        if (isSet(props.click().secretKey()) && isSandbox(props.click().apiUrl())) {
            errors.add("Click kaliti berilgan, lekin manzil sandbox: " + props.click().apiUrl());
        }
        return errors;
    }

    private static void requireHttps(List<String> errors, String name, String value) {
        if (!isSet(value)) {
            errors.add(name + " berilmagan");
        } else if (!value.trim().toLowerCase(Locale.ROOT).startsWith("https://")) {
            errors.add(name + " https:// bilan boshlanishi kerak: " + value);
        }
    }

    /** Sinov muhiti manzili (masalan https://test.paycom.uz) yoki manzil umuman berilmagan. */
    static boolean isSandbox(String url) {
        if (!isSet(url)) {
            return true;
        }
        String u = url.toLowerCase(Locale.ROOT);
        return u.contains("test") || u.contains("sandbox") || u.contains("dev.") || !u.startsWith("https://");
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
