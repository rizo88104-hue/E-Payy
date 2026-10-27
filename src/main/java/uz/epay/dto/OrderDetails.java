package uz.epay.dto;

import uz.epay.entity.Order;
import uz.epay.entity.OrderItem;
import uz.epay.entity.OrderStatus;
import uz.epay.entity.PaymentMethod;
import uz.epay.entity.PaymentStatus;
import uz.epay.entity.Region;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Xodim uchun to'liq buyurtma: mijoz, qatorlar (fiskal nusxa bilan), kurs, operator va izoh. */
public record OrderDetails(Long id, String number, String customerName, String phone, String address, Region region,
                           OrderStatus status, PaymentStatus payStatus, PaymentMethod paymentMethod,
                           List<Line> items, BigDecimal totalUsd, BigDecimal usdRate, long amountUzs,
                           Long operatorId, String operatorName, String operatorNote,
                           Instant createdAt, Instant expiresAt, Instant paidAt, Long version) {

    public record Line(Long productId, String productName, String sku, BigDecimal unitPriceUsd, int warrantyYears,
                       int quantity, BigDecimal lineTotalUsd, String mxikCode, String packageCode) {

        static Line from(OrderItem i) {
            return new Line(i.getProduct().getId(), i.getProductName(), i.getSku(), i.getUnitPriceUsd(),
                    i.getWarrantyYears(), i.getQuantity(), i.lineTotalUsd(), i.getMxikCode(), i.getPackageCode());
        }
    }

    public static OrderDetails from(Order o) {
        return new OrderDetails(o.getId(), o.getNumber(), o.getCustomerName(), o.getPhone(), o.getAddress(),
                o.getRegion(), o.getStatus(), o.getPayStatus(), o.getPaymentMethod(),
                o.getItems().stream().map(Line::from).toList(), o.getTotalUsd(), o.getUsdRate(), o.getAmountUzs(),
                o.getAssignedOperator() == null ? null : o.getAssignedOperator().getId(),
                o.getAssignedOperator() == null ? null : o.getAssignedOperator().getFullName(),
                o.getOperatorNote(), o.getCreatedAt(), o.getExpiresAt(), o.getPaidAt(), o.getVersion());
    }
}
