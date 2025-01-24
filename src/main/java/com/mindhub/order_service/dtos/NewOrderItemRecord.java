package com.mindhub.order_service.dtos;

public record NewOrderItemRecord(Long orderId,Long productId, Integer quantity) {
}