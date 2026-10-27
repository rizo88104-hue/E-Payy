package uz.epay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Xodim o'z parolini almashtiradi: joriy parol majburiy. */
public record ChangeOwnPasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 10, max = 72) String newPassword) {
}
