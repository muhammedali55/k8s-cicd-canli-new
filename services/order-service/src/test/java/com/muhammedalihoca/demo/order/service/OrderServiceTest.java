package com.muhammedalihoca.demo.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.muhammedalihoca.demo.order.client.InsufficientStockException;
import com.muhammedalihoca.demo.order.client.ProductClient;
import com.muhammedalihoca.demo.order.client.ProductNotFoundException;
import com.muhammedalihoca.demo.order.client.ProductResponse;
import com.muhammedalihoca.demo.order.client.ProductServiceUnavailableException;
import com.muhammedalihoca.demo.order.domain.Order;
import com.muhammedalihoca.demo.order.domain.OrderStatus;
import com.muhammedalihoca.demo.order.dto.CreateOrderRequest;
import com.muhammedalihoca.demo.order.dto.OrderResponse;
import com.muhammedalihoca.demo.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;

// OrderService'in sipariş akışını test eder. Veritabanı gerçek (H2 + Flyway),
// product-service ise ProductClient mock'lanarak taklit edilir.
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private ProductClient productClient;

    private static final ProductResponse KLAVYE =
            new ProductResponse(1L, "Mekanik Klavye", new BigDecimal("1299.90"), 25);

    @BeforeEach
    void temizle() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("Stok yeterliyse sipariş CREATED kaydedilir ve toplam fiyat = fiyat × adet olur")
    void basariliSiparis() {
        // given
        when(productClient.getProduct(1L)).thenReturn(KLAVYE);
        when(productClient.decreaseStock(1L, 2))
                .thenReturn(new ProductResponse(1L, "Mekanik Klavye", new BigDecimal("1299.90"), 23));

        // when
        OrderResponse order = orderService.createOrder(new CreateOrderRequest(1L, 2));

        // then
        assertThat(order.id()).isNotNull();
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.totalPrice()).isEqualByComparingTo("2599.80");
        assertThat(order.createdAt()).isNotNull();

        List<Order> saved = orderRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getStatus()).isEqualTo(OrderStatus.CREATED);
        verify(productClient).decreaseStock(1L, 2);
    }

    @Test
    @DisplayName("Stok yetersizse sipariş REJECTED kaydedilir ve hata sipariş id'sini taşır")
    void stokYetersiz() {
        // given
        when(productClient.getProduct(1L)).thenReturn(KLAVYE);
        when(productClient.decreaseStock(1L, 30))
                .thenThrow(new InsufficientStockException("Yetersiz stok: ürün 1 için istenen 30, mevcut 25"));

        // when / then
        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(1L, 30)))
                .isInstanceOf(OrderRejectedException.class)
                .hasMessageContaining("Yetersiz stok")
                .satisfies(e -> {
                    Long orderId = ((OrderRejectedException) e).getOrderId();
                    Order rejected = orderRepository.findById(orderId).orElseThrow();
                    assertThat(rejected.getStatus()).isEqualTo(OrderStatus.REJECTED);
                    assertThat(rejected.getQuantity()).isEqualTo(30);
                });
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("product-service kapalıysa ProductServiceUnavailableException fırlar ve sipariş kaydedilmez")
    void productServiceKapali() {
        // given
        when(productClient.getProduct(1L)).thenThrow(new ProductServiceUnavailableException(
                "product-service'e ulaşılamıyor, lütfen daha sonra tekrar deneyin",
                new ResourceAccessException("Connection refused")));

        // when / then
        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(1L, 2)))
                .isInstanceOf(ProductServiceUnavailableException.class);
        assertThat(orderRepository.count()).isZero();
        verify(productClient, never()).decreaseStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("Ürün yoksa ProductNotFoundException fırlar ve sipariş kaydedilmez")
    void urunYok() {
        // given
        when(productClient.getProduct(99L)).thenThrow(new ProductNotFoundException(99L));

        // when / then
        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(99L, 1)))
                .isInstanceOf(ProductNotFoundException.class);
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    @DisplayName("Olmayan sipariş id'si istenirse OrderNotFoundException fırlar")
    void siparisYok() {
        // given: boş tablo

        // when / then
        assertThatThrownBy(() -> orderService.findById(12345L))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
