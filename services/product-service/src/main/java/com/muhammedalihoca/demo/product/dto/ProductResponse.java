package com.muhammedalihoca.demo.product.dto;

import com.muhammedalihoca.demo.product.domain.Product;

import java.math.BigDecimal;

// Ürünün API'den dışarıya açılan hali (sözleşmedeki "Product" şeması).
public record ProductResponse(Long id, String name, BigDecimal price, Integer stock) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice(), product.getStock());
    }
}
