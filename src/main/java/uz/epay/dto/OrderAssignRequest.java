package uz.epay.dto;

import jakarta.validation.constraints.NotNull;

/** Admin: buyurtmani operatorga biriktirish. */
public record OrderAssignRequest(@NotNull Long operatorId) {
}
