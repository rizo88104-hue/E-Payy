package uz.epay.service;

import uz.epay.exception.BadRequestException;

/** O'zbekiston telefon raqamlari: tekshirish va bazadagi yagona format (+998XXXXXXXXX). */
public final class PhoneNumbers {
    public static final String INVALID = "Telefon raqam noto'g'ri. Masalan: +998 90 123 45 67";

    private PhoneNumbers() {
    }

    /** Faqat raqamlar olinadi: 998 + 9 raqam yoki 9 raqam. Boshqa hamma holat — BadRequestException. */
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

    /** Solishtirish uchun kalit: oxirgi 9 raqam (turli yozilish shakllari bir xil kalit beradi). */
    public static String key(String phone) {
        String d = digits(phone);
        return d.length() > 9 ? d.substring(d.length() - 9) : d;
    }

    /** Ekranda ko'rsatish uchun yashirilgan raqam: +998 90 *** ** 67. */
    public static String mask(String phone) {
        String n = normalize(phone);
        return "+998 " + n.substring(4, 6) + " *** ** " + n.substring(11);
    }

    private static String digits(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D", "");
    }
}
