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
import jakarta.persistence.PreUpdate;
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
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    /** Ixtiyoriy. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    /** Dollarda. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    /** Har qatorda "Nomi: qiymati" (masalan "RAM: 16GB"). */
    @Column(columnDefinition = "text")
    private String specs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProductStatus status = ProductStatus.DRAFT;

    /** Bir vaqtda tahrirlashdan himoya (optimistik blok). */
    @Version
    @Column(nullable = false)
    @Setter(lombok.AccessLevel.NONE)
    private Long version;

    /** Soliq: MXIK (IKPU) kodi, 17 raqam. */
    @Column(name = "mxik_code", length = 17)
    private String mxikCode;

    /** Soliq: qadoq (o'lchov birligi) kodi. */
    @Column(name = "package_code", length = 20)
    private String packageCode;

    /** QQS foizi, masalan 12.00. */
    @Column(name = "vat_percent", precision = 5, scale = 2)
    private BigDecimal vatPercent;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(lombok.AccessLevel.NONE)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @Setter(lombok.AccessLevel.NONE)
    private Instant updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc, id asc")
    @BatchSize(size = 100)
    @Setter(lombok.AccessLevel.NONE)
    private List<ProductImage> images = new ArrayList<>();

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Asosiy rasm (birinchisi) yoki null. */
    public String mainImageUrl() {
        return images.isEmpty() ? null : images.getFirst().getUrl();
    }

    /** Rasmni ro'yxat oxiriga qo'shadi. */
    public ProductImage addImage(String url) {
        ProductImage img = new ProductImage(this, url);
        images.add(img);
        reindexImages();
        return img;
    }

    /** Ro'yxat tartibi o'zgargach: sortOrder 0..n. */
    public void reindexImages() {
        for (int i = 0; i < images.size(); i++) {
            images.get(i).setSortOrder(i);
        }
    }
}
