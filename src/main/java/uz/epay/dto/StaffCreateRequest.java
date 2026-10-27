package uz.epay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uz.epay.entity.StaffRole;

public record StaffCreateRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{3,64}$", message = "3-64 ta lotin harf, raqam, nuqta, _ yoki -")
        String username,
        @NotBlank @Size(min = 10, max = 72) String password,
        @NotBlank @Size(max = 255) String fullName,
        String phone,
        @NotNull StaffRole role) {
}
