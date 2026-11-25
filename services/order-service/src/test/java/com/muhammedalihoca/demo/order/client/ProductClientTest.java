package com.muhammedalihoca.demo.order.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

// ProductClient'ın sözleşmedeki yanıtları doğru yorumladığını test eder.
// product-service yerine MockRestServiceServer sahte HTTP yanıtları üretir.
class ProductClientTest {

    private MockRestServiceServer server;
    private ProductClient productClient;

    @BeforeEach
    void hazirla() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://product-service:8080");
        server = MockRestServiceServer.bindTo(builder).build();
        productClient = new ProductClient(builder.build());
    }

    @Test
    @DisplayName("GET /api/products/{id} 200 dönerse ürün okunur")
    void urunGetir() {
        // given
        server.expect(requestTo("http://product-service:8080/api/products/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        { "id": 1, "name": "Mekanik Klavye", "price": 1299.90, "stock": 25 }
                        """, MediaType.APPLICATION_JSON));

        // when
        ProductResponse product = productClient.getProduct(1L);

        // then
        assertThat(product.name()).isEqualTo("Mekanik Klavye");
        assertThat(product.price()).isEqualByComparingTo("1299.90");
        server.verify();
    }

    @Test
    @DisplayName("decrease-stock doğru gövdeyle çağrılır ve güncel ürün döner")
    void stokDusur() {
        // given
        server.expect(requestTo("http://product-service:8080/api/products/1/decrease-stock"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        { "quantity": 2 }
                        """))
                .andRespond(withSuccess("""
                        { "id": 1, "name": "Mekanik Klavye", "price": 1299.90, "stock": 23 }
                        """, MediaType.APPLICATION_JSON));

        // when
        ProductResponse product = productClient.decreaseStock(1L, 2);

        // then
        assertThat(product.stock()).isEqualTo(23);
        server.verify();
    }

    @Test
    @DisplayName("404 yanıtı ProductNotFoundException'a çevrilir")
    void urunYok() {
        // given
        server.expect(requestTo("http://product-service:8080/api/products/99"))
                .andRespond(withResourceNotFound());

        // when / then
        assertThatThrownBy(() -> productClient.getProduct(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Ürün bulunamadı: 99");
    }

    @Test
    @DisplayName("409 yanıtı InsufficientStockException'a çevrilir; mesaj ProblemDetail'den gelir")
    void stokYetersiz() {
        // given
        server.expect(requestTo("http://product-service:8080/api/products/1/decrease-stock"))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                { "type": "about:blank", "title": "Conflict", "status": 409,
                                  "detail": "Yetersiz stok: ürün 1 için istenen 30, mevcut 25",
                                  "instance": "/api/products/1/decrease-stock" }
                                """));

        // when / then
        assertThatThrownBy(() -> productClient.decreaseStock(1L, 30))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Yetersiz stok: ürün 1 için istenen 30, mevcut 25");
    }

    @Test
    @DisplayName("product-service 5xx dönerse ProductServiceUnavailableException fırlar")
    void sunucuHatasi() {
        // given
        server.expect(requestTo("http://product-service:8080/api/products/1"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // when / then
        assertThatThrownBy(() -> productClient.getProduct(1L))
                .isInstanceOf(ProductServiceUnavailableException.class);
    }

    @Test
    @DisplayName("product-service'e bağlanılamazsa ProductServiceUnavailableException fırlar")
    void baglantiYok() {
        // given: 1 numaralı port'ta dinleyen kimse yok, bağlantı reddedilir
        ProductClient kapaliServiseBakan = new ProductClient(
                RestClient.builder().baseUrl("http://localhost:1").build());

        // when / then
        assertThatThrownBy(() -> kapaliServiseBakan.getProduct(1L))
                .isInstanceOf(ProductServiceUnavailableException.class)
                .hasMessageContaining("ulaşılamıyor");
    }
}
