package com.muhammedalihoca.demo.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// POST /api/products/{id}/decrease-stock gövdesi: { "quantity": n }, n > 0.
public record DecreaseStockRequest(
        @NotNull(message = "zorunlu")
        @Positive(message = "0'dan büyük olmalı")
        Integer quantity) {
}
