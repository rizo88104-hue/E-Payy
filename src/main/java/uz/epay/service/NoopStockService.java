package uz.epay.service;

import org.springframework.stereotype.Component;
import uz.epay.entity.Order;

@Component
class NoopStockService implements StockService {

    @Override
    public void reserve(Order order) {
    }

    @Override
    public void release(Order order) {
    }
}
