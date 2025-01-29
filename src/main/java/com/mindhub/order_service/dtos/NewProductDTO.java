package com.mindhub.order_service.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record NewProductDTO(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be a positive number")
        Double price,

        @NotNull(message = "Stock is required")
        @PositiveOrZero(message = "Stock must be a positive number or zero")
        Integer stock) {
}
