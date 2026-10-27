package uz.epay.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uz.epay.entity.ProductStatus;

import java.math.BigDecimal;

/**
 * Admin: mahsulot yaratish/tahrirlash. Narx dollarda.
 * version — tahrirda majburiy: mahsulot ochilgandagi qiymat; bazadagidan farq qilsa 409.
 * status berilmasa — DRAFT.
 */
public record ProductRequest(
        @NotBlank @Size(max = 64)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "faqat lotin harf, raqam, \".\", \"_\" va \"-\"")
        String sku,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 20_000) String description,
        @NotNull Long categoryId,
        Long brandId,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal price,
        @Size(max = 20_000) String specs,
        ProductStatus status,
        Long version,
        @Pattern(regexp = "^[0-9]{17}$", message = "17 ta raqamdan iborat bo'lishi kerak") String mxikCode,
        @Size(max = 20) @Pattern(regexp = "^[0-9]+$", message = "faqat raqamlar") String packageCode,
        @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal vatPercent) {
}
