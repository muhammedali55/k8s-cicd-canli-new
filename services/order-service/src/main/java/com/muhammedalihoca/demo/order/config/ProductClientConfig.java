package com.muhammedalihoca.demo.order.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// product-service'e giden RestClient'ı hazırlar: base URL (PRODUCT_SERVICE_URL) ve 2 sn timeout'lar.
// Spring'in hazır RestClient.Builder'ı kullanılır; böylece çağrılar Actuator metriklerine de düşer.
@Configuration
public class ProductClientConfig {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    @Bean
    public RestClient productRestClient(RestClient.Builder builder,
                                        @Value("${app.product-service.url}") String productServiceUrl) {
        // Bağlantı kurma timeout'u
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();

        // Yanıt okuma timeout'u
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(TIMEOUT);

        return builder
                .baseUrl(productServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
