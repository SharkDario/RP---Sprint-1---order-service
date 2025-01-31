package com.mindhub.order_service.dtos;

import java.util.List;

public record OrderCreatedRecord(OrderDTO orderDTO, List<ErrorProductRecord> errorProductRecordList) {
}
