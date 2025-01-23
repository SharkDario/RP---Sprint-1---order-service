package com.mindhub.order_service.dtos;

import com.mindhub.order_service.models.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderDTO(
        @NotNull(message = "Order status is required and cannot be null")
        OrderStatus status) {
}
