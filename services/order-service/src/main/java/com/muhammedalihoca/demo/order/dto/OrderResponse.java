package com.muhammedalihoca.demo.order.dto;

import com.muhammedalihoca.demo.order.domain.Order;
import com.muhammedalihoca.demo.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

// Siparişin dışarıya (JSON) açılan hali. Entity'yi doğrudan döndürmemek için kullanılır.
public record OrderResponse(
        Long id,
        Long productId,
        Integer quantity,
        BigDecimal totalPrice,
        OrderStatus status,
        Instant createdAt) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt());
    }
}
