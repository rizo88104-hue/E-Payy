package uz.epay.service;

import uz.epay.entity.Order;

/**
 * Buyurtma hodisasi (Spring ApplicationEvent). Tranzaksiya ICHIDA e'lon qilinadi — tinglovchi o'zi tanlaydi:
 * TODO(2.3): Outbox — @TransactionalEventListener(BEFORE_COMMIT) bilan outbox jadvaliga yoziladi
 * (SMS, Telegram, fiskal chek o'sha yerdan, buyurtma bilan bir tranzaksiyada).
 */
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
