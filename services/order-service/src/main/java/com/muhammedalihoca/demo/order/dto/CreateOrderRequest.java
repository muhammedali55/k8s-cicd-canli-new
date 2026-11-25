package com.muhammedalihoca.demo.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// POST /api/orders gövdesi: hangi üründen kaç adet.
public record CreateOrderRequest(
        @NotNull(message = "productId zorunlu") @Positive(message = "productId 0'dan büyük olmalı") Long productId,
        @NotNull(message = "quantity zorunlu") @Positive(message = "quantity 0'dan büyük olmalı") Integer quantity) {
}
