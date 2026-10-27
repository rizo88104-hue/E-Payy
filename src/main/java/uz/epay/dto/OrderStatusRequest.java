package uz.epay.dto;

import jakarta.validation.constraints.NotNull;
import uz.epay.entity.OrderStatus;

public record OrderStatusRequest(@NotNull OrderStatus status) {
}
