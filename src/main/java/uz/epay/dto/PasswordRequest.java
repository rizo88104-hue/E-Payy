package uz.epay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordRequest(@NotBlank @Size(min = 10, max = 72) String password) {
}
