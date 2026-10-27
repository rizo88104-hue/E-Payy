package uz.epay.service;

import uz.epay.entity.Order;

public interface StockService {

    void reserve(Order order);

    void release(Order order);
}
