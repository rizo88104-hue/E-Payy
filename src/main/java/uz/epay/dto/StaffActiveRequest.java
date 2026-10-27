package uz.epay.dto;

import jakarta.validation.constraints.NotNull;

public record StaffActiveRequest(@NotNull Boolean active) {
}
