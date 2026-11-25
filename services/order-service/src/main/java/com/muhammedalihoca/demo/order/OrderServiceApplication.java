package com.muhammedalihoca.demo.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// order-service'in giriş noktası: Spring Boot uygulamasını başlatır.
// Sipariş alır, stok için product-service'i çağırır.
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
