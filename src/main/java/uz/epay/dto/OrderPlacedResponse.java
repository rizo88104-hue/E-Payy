package uz.epay.dto;

import uz.epay.entity.Order;
import uz.epay.entity.OrderStatus;
import uz.epay.entity.PaymentMethod;
import uz.epay.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** POST /api/orders javobi xaridorga: faqat yangi buyurtma haqida. Ichki id va xodim maydonlari yo'q. */
public record OrderPlacedResponse(String number, OrderStatus status, PaymentStatus payStatus,
                                  PaymentMethod paymentMethod, BigDecimal totalUsd, long amountUzs,
                                  Instant createdAt, Instant expiresAt) {

    public static OrderPlacedResponse from(Order o) {
        return new OrderPlacedResponse(o.getNumber(), o.getStatus(), o.getPayStatus(), o.getPaymentMethod(),
                o.getTotalUsd(), o.getAmountUzs(), o.getCreatedAt(), o.getExpiresAt());
    }
}
