package com.muhammedalihoca.demo.order.client;

import java.math.BigDecimal;

// product-service'in döndürdüğü ürün (sözleşmedeki "Product" şeması).
public record ProductResponse(Long id, String name, BigDecimal price, Integer stock) {
}
