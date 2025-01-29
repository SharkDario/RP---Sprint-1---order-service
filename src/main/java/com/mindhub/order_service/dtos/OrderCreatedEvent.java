package com.mindhub.order_service.dtos;

import com.mindhub.order_service.models.OrderStatus;

import java.util.List;

public record OrderCreatedEvent(
        Long id,
        String email,
        OrderStatus status,
        List<NewProductDTO> products
) {
}
