package uz.epay.service;

/** Mahsulot buyurtmalarda ishlatilganmi: ishlatilgan bo'lsa o'chirilmaydi, ARCHIVED qilinadi. */
public interface ProductUsageChecker {

    boolean isUsedInOrders(Long productId);
}
