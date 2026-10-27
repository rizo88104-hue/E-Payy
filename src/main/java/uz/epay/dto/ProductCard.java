package uz.epay.dto;

import uz.epay.entity.Product;

import java.math.BigDecimal;

public record ProductCard(Long id, String sku, String name, BigDecimal price, String imageUrl,
                          Long categoryId, String categoryName, Long brandId, String brandName) {

    public static ProductCard from(Product p) {
        return new ProductCard(p.getId(), p.getSku(), p.getName(), p.getPrice(), p.mainImageUrl(),
                p.getCategory().getId(), p.getCategory().getName(),
                p.getBrand() == null ? null : p.getBrand().getId(),
                p.getBrand() == null ? null : p.getBrand().getName());
    }
}
