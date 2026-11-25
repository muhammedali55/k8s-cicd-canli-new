package com.muhammedalihoca.demo.product.exception;

// İstenen id'de ürün yoksa fırlatılır; GlobalExceptionHandler bunu 404'e çevirir.
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Ürün bulunamadı: " + id);
    }
}
