package com.mindhub.order_service.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateOrderItemDTO(
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be a positive number")
        Integer quantity) {
}
