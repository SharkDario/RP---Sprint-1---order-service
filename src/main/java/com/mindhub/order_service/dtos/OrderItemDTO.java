package com.mindhub.order_service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mindhub.order_service.models.OrderItem;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class OrderItemDTO {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private final Long id;

    private Long productId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be a positive number")
    private Integer quantity;

    public OrderItemDTO(OrderItem orderItem) {
        id = orderItem.getId();
        productId = orderItem.getProductId();
        quantity = orderItem.getQuantity();
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
