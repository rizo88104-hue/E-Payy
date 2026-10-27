package uz.epay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Xaridor buyurtmasini kuzatadi: raqam + telefon. */
public record OrderTrackRequest(@NotBlank @Size(max = 20) String number, @NotBlank @Size(max = 30) String phone) {
}
