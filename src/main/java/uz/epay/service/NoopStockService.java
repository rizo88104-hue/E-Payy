package uz.epay.service;

import org.springframework.stereotype.Component;
import uz.epay.entity.Order;

/** Vaqtinchalik: qoldiq hali yuritilmaydi. TODO(2.1): stocks jadvali bilan StockService realizatsiyasiga almashtirish. */
@Component
class NoopStockService implements StockService {

    @Override
    public void reserve(Order order) {
        // TODO(2.1): qoldiqni tekshirish va band qilish
    }

    @Override
    public void release(Order order) {
        // TODO(2.1): qoldiqni qaytarish va order.setStockReturned(true)
    }
}
