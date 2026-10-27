package uz.epay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderTrackRequest(@NotBlank @Size(max = 20) String number, @NotBlank @Size(max = 30) String phone) {
}
