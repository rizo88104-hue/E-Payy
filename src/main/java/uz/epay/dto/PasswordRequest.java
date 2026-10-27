package uz.epay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin: xodim parolini almashtirish. */
public record PasswordRequest(@NotBlank @Size(min = 10, max = 72) String password) {
}
