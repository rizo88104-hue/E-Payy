package uz.epay.dto;

import jakarta.validation.constraints.NotNull;

public record OrderAssignRequest(@NotNull Long operatorId) {
}
