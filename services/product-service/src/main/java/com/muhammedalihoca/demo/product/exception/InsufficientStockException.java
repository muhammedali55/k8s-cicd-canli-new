package com.muhammedalihoca.demo.product.exception;

// Stok, istenen miktardan azsa fırlatılır; GlobalExceptionHandler bunu 409'a çevirir.
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int requested, int available) {
        super("Yetersiz stok: ürün " + productId + " için istenen " + requested + ", mevcut " + available);
    }
}
