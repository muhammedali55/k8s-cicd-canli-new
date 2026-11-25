package com.muhammedalihoca.demo.order.client;

import java.util.function.Supplier;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

// product-service'i HTTP ile çağıran istemci (contracts/product-api.yaml).
// HTTP hatalarını order-service'in anlayacağı exception'lara çevirir.
@Component
public class ProductClient {

    private final RestClient restClient;

    // RestClient, ProductClientConfig'te base URL ve timeout'larla hazırlanır
    public ProductClient(RestClient productRestClient) {
        this.restClient = productRestClient;
    }

    /** GET /api/products/{id} */
    public ProductResponse getProduct(Long productId) {
        return call(productId, () -> restClient.get()
                .uri("/api/products/{id}", productId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(ProductResponse.class));
    }

    /** POST /api/products/{id}/decrease-stock — başarıda ürünün güncel halini döner. */
    public ProductResponse decreaseStock(Long productId, int quantity) {
        return call(productId, () -> restClient.post()
                .uri("/api/products/{id}/decrease-stock", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(new DecreaseStockRequest(quantity))
                .retrieve()
                .body(ProductResponse.class));
    }

    // Tüm çağrılar için ortak hata çevirisi
    private ProductResponse call(Long productId, Supplier<ProductResponse> request) {
        try {
            return request.get();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductNotFoundException(productId);
        } catch (HttpClientErrorException.Conflict e) {
            throw new InsufficientStockException(problemDetailOf(e, "Yetersiz stok: ürün " + productId));
        } catch (HttpServerErrorException e) {
            throw new ProductServiceUnavailableException(
                    "product-service hata döndü (" + e.getStatusCode().value() + "), lütfen daha sonra tekrar deneyin", e);
        } catch (ResourceAccessException e) {
            // Bağlantı reddedildi, DNS çözülemedi ya da 2 saniyelik timeout doldu
            throw new ProductServiceUnavailableException(
                    "product-service'e ulaşılamıyor, lütfen daha sonra tekrar deneyin", e);
        }
    }

    // product-service'in ProblemDetail gövdesindeki "detail" alanını okur; okunamazsa varsayılanı döner
    private String problemDetailOf(HttpClientErrorException e, String fallback) {
        try {
            ProblemDetail problem = e.getResponseBodyAs(ProblemDetail.class);
            if (problem != null && problem.getDetail() != null) {
                return problem.getDetail();
            }
        } catch (RuntimeException ignored) {
            // Gövde ProblemDetail değilse varsayılan mesajla devam ederiz
        }
        return fallback;
    }
}
