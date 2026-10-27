package uz.epay.service;

import org.springframework.stereotype.Component;

/**
 * Vaqtinchalik realizatsiya: buyurtmalar jadvali hali yo'q, shuning uchun hech bir mahsulot ishlatilmagan.
 * TODO(1.4): orders/order_items paydo bo'lganda order_items.product_id bo'yicha haqiqiy tekshiruv bilan almashtirish
 *  va ARCHIVED holati uchun test qo'shish.
 */
@Component
class NoOrdersProductUsageChecker implements ProductUsageChecker {

    @Override
    public boolean isUsedInOrders(Long productId) {
        return false;
    }
}
