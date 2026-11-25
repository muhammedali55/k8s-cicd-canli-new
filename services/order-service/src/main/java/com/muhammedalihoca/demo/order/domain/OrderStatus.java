package com.muhammedalihoca.demo.order.domain;

// Siparişin durumu: stok düşürüldüyse CREATED, stok yetmediyse REJECTED.
public enum OrderStatus {
    CREATED,
    REJECTED
}
