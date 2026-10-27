package uz.epay.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class OrderTransitions {
    private static final Map<OrderStatus, Set<OrderStatus>> STATUS = Map.of(
            OrderStatus.NEW, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED),
            OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));

    private static final Map<PaymentStatus, Set<PaymentStatus>> PAYMENT = Map.of(
            PaymentStatus.UNPAID, EnumSet.of(PaymentStatus.PAID),
            PaymentStatus.PAID, EnumSet.of(PaymentStatus.REFUND_PENDING),
            PaymentStatus.REFUND_PENDING, EnumSet.of(PaymentStatus.REFUNDED),
            PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class));

    private OrderTransitions() {
    }

    public static boolean canMove(OrderStatus from, OrderStatus to) {
        return STATUS.get(from).contains(to);
    }

    public static boolean canMove(PaymentStatus from, PaymentStatus to) {
        return PAYMENT.get(from).contains(to);
    }

    public static boolean isFinal(OrderStatus status) {
        return STATUS.get(status).isEmpty();
    }
}
