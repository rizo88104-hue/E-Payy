package uz.epay.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin: kategoriya yaratish/tahrirlash. sortOrder berilmasa — 0. */
public record CategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @Min(0) Integer sortOrder) {
}
