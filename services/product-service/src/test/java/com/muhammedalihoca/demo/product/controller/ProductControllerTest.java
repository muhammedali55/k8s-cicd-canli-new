package com.muhammedalihoca.demo.product.controller;

import com.muhammedalihoca.demo.product.dto.CreateProductRequest;
import com.muhammedalihoca.demo.product.dto.ProductResponse;
import com.muhammedalihoca.demo.product.exception.InsufficientStockException;
import com.muhammedalihoca.demo.product.exception.ProductNotFoundException;
import com.muhammedalihoca.demo.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ProductController'ın HTTP davranışını (durum kodları, JSON, ProblemDetail) test eder.
// @WebMvcTest sadece web katmanını ayağa kaldırır; servis Mockito ile taklit edilir.
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    @DisplayName("GET /api/products tüm ürünleri döner")
    void listProducts_returnsAll() throws Exception {
        // given
        when(productService.findAll()).thenReturn(List.of(
                new ProductResponse(1L, "Mekanik Klavye", new BigDecimal("1299.90"), 25),
                new ProductResponse(2L, "Kablosuz Mouse", new BigDecimal("449.50"), 40)));

        // when / then
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Mekanik Klavye"))
                .andExpect(jsonPath("$[1].stock").value(40));
    }

    @Test
    @DisplayName("GET /api/products/{id} olmayan ürün için 404 ProblemDetail döner")
    void getProduct_whenMissing_returns404() throws Exception {
        // given
        when(productService.findById(99L)).thenThrow(new ProductNotFoundException(99L));

        // when / then
        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Ürün bulunamadı: 99"))
                .andExpect(jsonPath("$.instance").value("/api/products/99"));
    }

    @Test
    @DisplayName("POST /api/products geçerli istekte 201 ve Location başlığı döner")
    void createProduct_whenValid_returns201() throws Exception {
        // given
        when(productService.create(any(CreateProductRequest.class)))
                .thenReturn(new ProductResponse(4L, "USB-C Hub", new BigDecimal("799.00"), 15));

        // when / then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "USB-C Hub", "price": 799.00, "stock": 15 }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/products/4"))
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.name").value("USB-C Hub"));
    }

    @Test
    @DisplayName("POST /api/products fiyat 0 ise 400 ProblemDetail döner")
    void createProduct_whenPriceZero_returns400() throws Exception {
        // given
        String body = """
                { "name": "USB-C Hub", "price": 0, "stock": 15 }
                """;

        // when / then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Geçersiz istek: price 0'dan büyük olmalı"));

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("POST decrease-stock başarılıysa 200 ve güncel ürünü döner")
    void decreaseStock_whenEnoughStock_returns200() throws Exception {
        // given
        when(productService.decreaseStock(1L, 2))
                .thenReturn(new ProductResponse(1L, "Mekanik Klavye", new BigDecimal("1299.90"), 23));

        // when / then
        mockMvc.perform(post("/api/products/1/decrease-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "quantity": 2 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(23));
    }

    @Test
    @DisplayName("POST decrease-stock stok yetersizse 409 ProblemDetail döner")
    void decreaseStock_whenNotEnoughStock_returns409() throws Exception {
        // given
        when(productService.decreaseStock(1L, 30))
                .thenThrow(new InsufficientStockException(1L, 30, 25));

        // when / then
        mockMvc.perform(post("/api/products/1/decrease-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "quantity": 30 }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Yetersiz stok: ürün 1 için istenen 30, mevcut 25"));
    }

    @Test
    @DisplayName("POST decrease-stock miktar 0 ise 400 döner")
    void decreaseStock_whenQuantityZero_returns400() throws Exception {
        // when / then
        mockMvc.perform(post("/api/products/1/decrease-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "quantity": 0 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Geçersiz istek: quantity 0'dan büyük olmalı"));

        verifyNoInteractions(productService);
    }
}
