package uz.epay.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Admin: brend yaratish/tahrirlash. */
public record BrandRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 64)
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "faqat kichik lotin harf, raqam va \"-\"")
        String slug,
        @Size(max = 255) String logo,
        @Size(max = 1000) String description,
        @Size(max = 100) String country,
        @Min(1800) @Max(2100) Integer founded,
        @Size(max = 255) @Pattern(regexp = "^https?://.+", message = "http:// yoki https:// bilan boshlansin")
        String website) {
}
