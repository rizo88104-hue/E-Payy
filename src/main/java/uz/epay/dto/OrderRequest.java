package uz.epay.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import uz.epay.entity.PaymentMethod;
import uz.epay.entity.Region;

import java.util.List;

/**
 * Xaridor buyurtmasi. Narx yo'q — u faqat serverda hisoblanadi. Telefon formati va takroriy qatorlar
 * (birlashtirilgandan keyingi miqdor) OrderService da tekshiriladi. paymentMethod berilmasa — CASH.
 */
public record OrderRequest(
        @NotBlank @Size(min = 2, max = 100) String customerName,
        @NotBlank String phone,
        @NotBlank @Size(min = 5, max = 300) String address,
        @NotNull Region region,
        PaymentMethod paymentMethod,
        @NotEmpty @Size(max = 20) @Valid List<Item> items) {

    /** warrantyYears berilmasa — narx ichidagi eng qisqa kafolat. */
    public record Item(@NotNull Long productId, @Min(1) @Max(10) int quantity, Integer warrantyYears) {
    }
}
