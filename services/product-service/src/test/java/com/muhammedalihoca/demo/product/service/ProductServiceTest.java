package com.muhammedalihoca.demo.product.service;

import com.muhammedalihoca.demo.product.domain.Product;
import com.muhammedalihoca.demo.product.dto.ProductResponse;
import com.muhammedalihoca.demo.product.exception.InsufficientStockException;
import com.muhammedalihoca.demo.product.exception.ProductNotFoundException;
import com.muhammedalihoca.demo.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ProductService'in stok kurallarını, veritabanı olmadan (Mockito ile) test eder.
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("Stok yeterliyse istenen miktar düşülür ve güncel ürün döner")
    void decreaseStock_whenEnoughStock_decreasesAndReturnsProduct() {
        // given
        Product product = new Product("Mekanik Klavye", new BigDecimal("1299.90"), 25);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ProductResponse result = productService.decreaseStock(1L, 2);

        // then
        assertThat(result.stock()).isEqualTo(23);
        assertThat(product.getStock()).isEqualTo(23);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Stok yetersizse 409 için hata fırlatılır ve stok değişmez")
    void decreaseStock_whenNotEnoughStock_throwsAndKeepsStock() {
        // given
        Product product = new Product("Mekanik Klavye", new BigDecimal("1299.90"), 25);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // when / then
        assertThatThrownBy(() -> productService.decreaseStock(1L, 30))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Yetersiz stok: ürün 1 için istenen 30, mevcut 25");

        assertThat(product.getStock()).isEqualTo(25);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Stoğun tamamı düşülebilir, stok 0 olur")
    void decreaseStock_whenQuantityEqualsStock_stockBecomesZero() {
        // given
        Product product = new Product("27 inç Monitör", new BigDecimal("6999.00"), 10);
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ProductResponse result = productService.decreaseStock(3L, 10);

        // then
        assertThat(result.stock()).isZero();
    }

    @Test
    @DisplayName("Olmayan ürünün stoğu düşürülmek istenirse ProductNotFoundException fırlatılır")
    void decreaseStock_whenProductMissing_throwsNotFound() {
        // given
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> productService.decreaseStock(99L, 1))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Ürün bulunamadı: 99");
    }
}
