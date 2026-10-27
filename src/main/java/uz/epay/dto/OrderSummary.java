package uz.epay.dto;

import uz.epay.entity.Order;
import uz.epay.entity.OrderStatus;
import uz.epay.entity.PaymentMethod;
import uz.epay.entity.PaymentStatus;
import uz.epay.entity.Region;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummary(Long id, String number, String customerName, String phone, Region region,
                           OrderStatus status, PaymentStatus payStatus, PaymentMethod paymentMethod,
                           BigDecimal totalUsd, long amountUzs, Long operatorId, String operatorName,
                           Instant createdAt) {

    public static OrderSummary from(Order o) {
        return new OrderSummary(o.getId(), o.getNumber(), o.getCustomerName(), o.getPhone(), o.getRegion(),
                o.getStatus(), o.getPayStatus(), o.getPaymentMethod(), o.getTotalUsd(), o.getAmountUzs(),
                o.getAssignedOperator() == null ? null : o.getAssignedOperator().getId(),
                o.getAssignedOperator() == null ? null : o.getAssignedOperator().getFullName(),
                o.getCreatedAt());
    }
}
