package uz.epay.dto;

import uz.epay.entity.Order;
import uz.epay.entity.OrderStatus;
import uz.epay.entity.PaymentMethod;
import uz.epay.entity.PaymentStatus;
import uz.epay.service.PhoneNumbers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderTrackResponse(String number, OrderStatus status, PaymentStatus payStatus,
                                 PaymentMethod paymentMethod, List<Line> items, BigDecimal totalUsd, long amountUzs,
                                 String phoneMasked, Instant createdAt) {

    public record Line(String productName, int quantity, int warrantyYears, BigDecimal lineTotalUsd) {
    }

    public static OrderTrackResponse from(Order o) {
        return new OrderTrackResponse(o.getNumber(), o.getStatus(), o.getPayStatus(), o.getPaymentMethod(),
                o.getItems().stream().map(i -> new Line(i.getProductName(), i.getQuantity(), i.getWarrantyYears(),
                        i.lineTotalUsd())).toList(),
                o.getTotalUsd(), o.getAmountUzs(), PhoneNumbers.mask(o.getPhone()), o.getCreatedAt());
    }
}
