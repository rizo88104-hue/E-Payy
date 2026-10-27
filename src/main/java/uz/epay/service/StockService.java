package uz.epay.service;

import uz.epay.entity.Order;

/**
 * Ombor qoldig'i. Hozircha qoldiq tekshirilmaydi (NoopStockService).
 * TODO(2.1): stocks jadvali bilan haqiqiy realizatsiya — reserve() yetarli bo'lmasa BadRequest otadi
 *  (atomik UPDATE ... WHERE qty >= ?), release() qaytaradi va order.setStockReturned(true) qiladi.
 */
public interface StockService {

    /** Buyurtma qatorlari uchun qoldiqni band qiladi (OrderService.create tranzaksiyasi ichida). */
    void reserve(Order order);

    /** Bekor qilingan buyurtmaning qoldig'ini qaytaradi (bir marta: stockReturned). */
    void release(Order order);
}
