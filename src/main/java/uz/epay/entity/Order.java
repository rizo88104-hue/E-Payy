package uz.epay.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20, updatable = false)
    private String number;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(nullable = false, length = 13)
    private String phone;

    @Column(nullable = false, length = 300)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus status = OrderStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_status", nullable = false, length = 16)
    private PaymentStatus payStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 8)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Column(name = "total_usd", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal totalUsd;

    @Column(name = "usd_rate", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal usdRate;

    @Column(name = "amount_uzs", nullable = false, updatable = false)
    private long amountUzs;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_operator_id")
    private Staff assignedOperator;

    @Column(name = "operator_note", length = 1000)
    private String operatorNote;

    @Column(name = "cancel_token_hash", length = 100)
    private String cancelTokenHash;

    @Column(name = "cancel_token_expires_at")
    private Instant cancelTokenExpiresAt;

    @Column(name = "stock_returned", nullable = false)
    private boolean stockReturned;

    @Version
    @Column(nullable = false)
    @Setter(lombok.AccessLevel.NONE)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(lombok.AccessLevel.NONE)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    @BatchSize(size = 100)
    @Setter(lombok.AccessLevel.NONE)
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }
}
