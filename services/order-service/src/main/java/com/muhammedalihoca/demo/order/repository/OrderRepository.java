package com.muhammedalihoca.demo.order.repository;

import com.muhammedalihoca.demo.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

// Sipariş tablosuna erişim. Spring Data JPA metotları (save, findAll, findById) yeterli.
public interface OrderRepository extends JpaRepository<Order, Long> {
}
