package com.mindhub.order_service.dtos;

import java.util.List;

public record NewOrderRecord(String email, List<ProductQuantityRecord> recordList) {
}
