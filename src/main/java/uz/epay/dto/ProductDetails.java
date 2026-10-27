package uz.epay.dto;

import uz.epay.entity.Product;
import uz.epay.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Mahsulot sahifasi va admin tahriri. version — tahrirda qaytarib yuboriladi (optimistik blok). */
public record ProductDetails(Long id, String sku, String name, String description, BigDecimal price, String specs,
                             ProductStatus status, Long version, CategoryDto category, BrandDto brand,
                             List<ImageDto> images, String mxikCode, String packageCode, BigDecimal vatPercent,
                             Instant createdAt, Instant updatedAt) {

    public static ProductDetails from(Product p) {
        return new ProductDetails(p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getPrice(), p.getSpecs(),
                p.getStatus(), p.getVersion(), CategoryDto.from(p.getCategory()),
                p.getBrand() == null ? null : BrandDto.from(p.getBrand()),
                p.getImages().stream().map(ImageDto::from).toList(),
                p.getMxikCode(), p.getPackageCode(), p.getVatPercent(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
