package uz.epay.dto;

import jakarta.validation.constraints.Size;

/** Operator izohi; bo'sh yoki null — izoh o'chiriladi. */
public record OrderNoteRequest(@Size(max = 1000) String note) {
}
