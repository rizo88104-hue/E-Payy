package uz.epay.entity;

/** Buyurtma holati. O'tishlar — faqat OrderTransitions da. DELIVERED va CANCELLED — yakuniy. */
public enum OrderStatus {
    NEW,
    CONFIRMED,
    DELIVERED,
    CANCELLED
}
