package com.muhammedalihoca.demo.order.client;

// product-service'e gönderilen stok düşürme gövdesi (sözleşmedeki "DecreaseStockRequest").
public record DecreaseStockRequest(Integer quantity) {
}
