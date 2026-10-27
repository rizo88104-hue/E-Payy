package uz.epay.dto;

import jakarta.validation.constraints.Size;

public record OrderNoteRequest(@Size(max = 1000) String note) {
}
