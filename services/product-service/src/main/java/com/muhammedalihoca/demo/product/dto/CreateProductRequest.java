package com.muhammedalihoca.demo.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

// POST /api/products gövdesi. Kurallar bozulursa 400 (ProblemDetail) döner.
public record CreateProductRequest(
        @NotBlank(message = "boş olamaz")
        String name,

        @NotNull(message = "zorunlu")
        @Positive(message = "0'dan büyük olmalı")
        BigDecimal price,

        @NotNull(message = "zorunlu")
        @PositiveOrZero(message = "0 veya daha büyük olmalı")
        Integer stock) {
}
