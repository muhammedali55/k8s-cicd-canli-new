package com.muhammedalihoca.demo.order.client;

// product-service 404 döndüğünde fırlatılır: sipariş verilen ürün yok.
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("Ürün bulunamadı: " + productId);
    }
}
