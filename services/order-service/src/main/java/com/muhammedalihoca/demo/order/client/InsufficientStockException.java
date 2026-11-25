package com.muhammedalihoca.demo.order.client;

// product-service decrease-stock çağrısı 409 döndüğünde fırlatılır: stok yetersiz.
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
