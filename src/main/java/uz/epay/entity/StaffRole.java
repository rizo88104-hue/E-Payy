package uz.epay.entity;

/** Xodim rollari (TZ 7-bo'lim). Spring Security'da "ROLE_" + name(). */
public enum StaffRole {
    /** hamma narsa: katalog, ombor, buyurtmalar, pul qaytarish, analitika, xodimlar va sozlamalar */
    ADMIN,
    /** buyurtmalar bilan ishlaydi; katalogni faqat ko'radi */
    OPERATOR,
    /** ombor va mavjud mahsulotlarni tahrirlash/nashr qilish */
    WAREHOUSE,
    /** egasi: hammasini ko'radi, hech narsani o'zgartirmaydi; analitika */
    OWNER
}
