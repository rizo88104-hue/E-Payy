package uz.epay.service;

import uz.epay.entity.Order;

public record OrderEvent(Type type, Long orderId, String number) {

    public enum Type {
        CREATED,
        CONFIRMED,
        PAID,
        CANCELLED
    }

    static OrderEvent of(Type type, Order order) {
        return new OrderEvent(type, order.getId(), order.getNumber());
    }
}
