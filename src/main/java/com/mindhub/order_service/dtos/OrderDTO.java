package com.mindhub.order_service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mindhub.order_service.models.EntityOrder;
import com.mindhub.order_service.models.OrderItem;
import com.mindhub.order_service.models.OrderStatus;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class OrderDTO {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private final Long id;

    @NotBlank(message = "User is required")
    private Long userId;

    private List<OrderItemDTO> products;

    private OrderStatus status;

    public OrderDTO(EntityOrder order) {
        id = order.getId();
        userId = order.getUserId();
        status = order.getStatus();
        products = order
                .getProducts() // Set<OrderItem>
                .stream() // Stream<>
                .map(orderItem -> new OrderItemDTO(orderItem)) // Stream<OrderItem> Function Lambda
                .toList();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public List<OrderItemDTO> getProducts() {
        return products;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<ProductQuantityRecord> getProductQuantityRecords() {
        return this.products.stream()
                .map(orderItem -> new ProductQuantityRecord(
                        orderItem.getProductId(),
                        orderItem.getQuantity()
                ))
                .toList();
    }
}
