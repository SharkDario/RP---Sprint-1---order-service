package com.mindhub.order_service.dtos;

import com.mindhub.order_service.models.ProductError;

public record ErrorProductRecord(Long id, ProductError productError) {
}
