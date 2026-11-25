package com.muhammedalihoca.demo.order.client;

// product-service'e ulaşılamadığında (bağlantı hatası, timeout, 5xx) fırlatılır. Kullanıcıya 503 olarak döner.
public class ProductServiceUnavailableException extends RuntimeException {

    public ProductServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
