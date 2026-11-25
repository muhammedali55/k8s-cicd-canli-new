package com.muhammedalihoca.demo.product.repository;

import com.muhammedalihoca.demo.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

// Product tablosuna erişim. findAll, findById, save gibi metotlar Spring Data'dan hazır gelir.
public interface ProductRepository extends JpaRepository<Product, Long> {
}
