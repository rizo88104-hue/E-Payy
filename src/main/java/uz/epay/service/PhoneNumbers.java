package uz.epay.service;

import uz.epay.exception.BadRequestException;

public final class PhoneNumbers {
    public static final String INVALID = "Telefon raqam noto'g'ri. Masalan: +998 90 123 45 67";

    private PhoneNumbers() {
    }

    public static String normalize(String phone) {
        String d = digits(phone);
        if (d.length() == 12 && d.startsWith("998")) {
            d = d.substring(3);
        }
        if (d.length() != 9) {
            throw new BadRequestException(INVALID);
        }
        return "+998" + d;
    }

    public static String key(String phone) {
        String d = digits(phone);
        return d.length() > 9 ? d.substring(d.length() - 9) : d;
    }

    public static String mask(String phone) {
        String n = normalize(phone);
        return "+998 " + n.substring(4, 6) + " *** ** " + n.substring(11);
    }

    private static String digits(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D", "");
    }
}
