package uz.epay.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Mahsulotning barcha rasm id lari yangi tartibda; birinchisi asosiy bo'ladi. */
public record ImageOrderRequest(@NotEmpty List<Long> ids) {
}
