package com.muhammedalihoca.demo.order.service;

// Stok yetersiz olduğu için sipariş REJECTED kaydedildiğinde fırlatılır; 409 olarak döner.
// Kaydedilen siparişin id'sini taşır ki yanıtta "orderId" alanı olarak gösterilebilsin.
public class OrderRejectedException extends RuntimeException {

    private final Long orderId;

    public OrderRejectedException(Long orderId, String message) {
        super(message);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
