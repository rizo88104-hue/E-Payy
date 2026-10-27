package uz.epay.dto;

import jakarta.validation.constraints.NotNull;

/** Admin: bloklash (false) yoki blokdan chiqarish (true). */
public record StaffActiveRequest(@NotNull Boolean active) {
}
