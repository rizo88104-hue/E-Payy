package uz.epay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Buyurtma qatori. Nom, SKU, narx va soliq kodlari buyurtma paytidagi holatida NUSXALANADI —
 * mahsulot keyin o'zgarsa ham chek va tarix o'zgarmaydi.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false, length = 64)
    private String sku;

    /** Bir dona narxi, dollarda, tanlangan kafolat bilan. */
    @Column(name = "unit_price_usd", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPriceUsd;

    @Column(name = "warranty_years", nullable = false)
    private int warrantyYears;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "mxik_code", length = 17)
    private String mxikCode;

    @Column(name = "package_code", length = 20)
    private String packageCode;

    public BigDecimal lineTotalUsd() {
        return unitPriceUsd.multiply(BigDecimal.valueOf(quantity));
    }
}
