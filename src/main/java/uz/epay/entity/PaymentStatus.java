package uz.epay.entity;

/** To'lov holati. REFUND_PENDING — to'langan buyurtma bekor qilindi, pul hali qaytarilmagan (admin REFUNDED qiladi). */
public enum PaymentStatus {
    UNPAID,
    PAID,
    REFUND_PENDING,
    REFUNDED
}
