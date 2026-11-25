package com.muhammedalihoca.demo.product.service;

import com.muhammedalihoca.demo.product.domain.Product;
import com.muhammedalihoca.demo.product.dto.CreateProductRequest;
import com.muhammedalihoca.demo.product.dto.ProductResponse;
import com.muhammedalihoca.demo.product.exception.InsufficientStockException;
import com.muhammedalihoca.demo.product.exception.ProductNotFoundException;
import com.muhammedalihoca.demo.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Ürün iş kuralları: listeleme, oluşturma ve stok düşürme.
// Controller'a entity değil, ProductResponse (record) döner.
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.from(getProductOrThrow(id));
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Product product = new Product(request.name(), request.price(), request.stock());
        return ProductResponse.from(productRepository.save(product));
    }

    // Stok yeterliyse düşürür ve güncel ürünü döner; yetmezse hiçbir şey değiştirmeden 409 için hata fırlatır.
    // Not: Demo basit tutulduğu için eşzamanlı isteklere karşı kilit kullanılmıyor.
    @Transactional
    public ProductResponse decreaseStock(Long id, int quantity) {
        Product product = getProductOrThrow(id);

        if (product.getStock() < quantity) {
            throw new InsufficientStockException(id, quantity, product.getStock());
        }

        product.setStock(product.getStock() - quantity);
        return ProductResponse.from(productRepository.save(product));
    }

    private Product getProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
