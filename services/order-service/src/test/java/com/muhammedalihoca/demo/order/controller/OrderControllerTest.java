package com.muhammedalihoca.demo.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.muhammedalihoca.demo.order.client.ProductNotFoundException;
import com.muhammedalihoca.demo.order.client.ProductServiceUnavailableException;
import com.muhammedalihoca.demo.order.domain.OrderStatus;
import com.muhammedalihoca.demo.order.dto.CreateOrderRequest;
import com.muhammedalihoca.demo.order.dto.OrderResponse;
import com.muhammedalihoca.demo.order.service.OrderNotFoundException;
import com.muhammedalihoca.demo.order.service.OrderRejectedException;
import com.muhammedalihoca.demo.order.service.OrderService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// OrderController'ın HTTP davranışını (durum kodları, JSON, ProblemDetail) test eder.
// Yalnızca web katmanı ayağa kalkar; OrderService mock'tur.
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private static final String GECERLI_GOVDE = """
            { "productId": 1, "quantity": 2 }
            """;

    @Test
    @DisplayName("POST /api/orders başarılıysa 201, Location başlığı ve sipariş döner")
    void siparisOlustur() throws Exception {
        // given
        when(orderService.createOrder(new CreateOrderRequest(1L, 2))).thenReturn(new OrderResponse(
                10L, 1L, 2, new BigDecimal("2599.80"), OrderStatus.CREATED, Instant.parse("2026-01-01T10:00:00Z")));

        // when / then
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(GECERLI_GOVDE))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/orders/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalPrice").value(2599.80));
    }

    @Test
    @DisplayName("Stok yetersizse 409 ProblemDetail ve orderId alanı döner")
    void stokYetersiz() throws Exception {
        // given
        when(orderService.createOrder(any()))
                .thenThrow(new OrderRejectedException(7L, "Yetersiz stok: ürün 1 için istenen 30, mevcut 25"));

        // when / then
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(GECERLI_GOVDE))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Yetersiz stok: ürün 1 için istenen 30, mevcut 25"))
                .andExpect(jsonPath("$.orderId").value(7));
    }

    @Test
    @DisplayName("product-service erişilemezse 503 ProblemDetail döner")
    void productServiceKapali() throws Exception {
        // given
        when(orderService.createOrder(any())).thenThrow(new ProductServiceUnavailableException(
                "product-service'e ulaşılamıyor, lütfen daha sonra tekrar deneyin", null));

        // when / then
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(GECERLI_GOVDE))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value("product-service'e ulaşılamıyor, lütfen daha sonra tekrar deneyin"));
    }

    @Test
    @DisplayName("Olmayan ürün için sipariş verilirse 404 ProblemDetail döner")
    void urunYok() throws Exception {
        // given
        when(orderService.createOrder(any())).thenThrow(new ProductNotFoundException(99L));

        // when / then
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productId": 99, "quantity": 1 }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Ürün bulunamadı: 99"));
    }

    @Test
    @DisplayName("quantity 0 ise 400 ProblemDetail döner")
    void gecersizGovde() throws Exception {
        // given
        String govde = """
                { "productId": 1, "quantity": 0 }
                """;

        // when / then
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(govde))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Geçersiz istek: quantity 0'dan büyük olmalı"));
    }

    @Test
    @DisplayName("GET /api/orders tüm siparişleri döner")
    void tumSiparisler() throws Exception {
        // given
        when(orderService.findAll()).thenReturn(List.of(
                new OrderResponse(1L, 1L, 2, new BigDecimal("2599.80"), OrderStatus.CREATED, Instant.now()),
                new OrderResponse(2L, 1L, 30, new BigDecimal("38997.00"), OrderStatus.REJECTED, Instant.now())));

        // when / then
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].status").value("REJECTED"));
    }

    @Test
    @DisplayName("GET /api/orders/{id} sipariş yoksa 404 ProblemDetail döner")
    void siparisYok() throws Exception {
        // given
        when(orderService.findById(42L)).thenThrow(new OrderNotFoundException(42L));

        // when / then
        mockMvc.perform(get("/api/orders/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Sipariş bulunamadı: 42"));
    }
}
