package com.muhammedalihoca.demo.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Uygulamanın tamamını H2 ("test" profili) ile ayağa kaldırır:
// Flyway migration'ları, örnek ürünler, /api/info ve Actuator probe'ları gerçekten çalışıyor mu?
class ProductServiceApplicationTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    @DisplayName("Normal sürüm")
    class NormalVersion {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("Flyway 3 örnek ürünü yükler")
        void sampleProductsAreLoaded() throws Exception {
            // when / then
            mockMvc.perform(get("/api/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].name").value("Mekanik Klavye"));
        }

        @Test
        @DisplayName("APP_VERSION yoksa /api/info sürümü 'local' döner")
        void infoReturnsLocalVersion() throws Exception {
            // when / then
            mockMvc.perform(get("/api/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.service").value("product-service"))
                    .andExpect(jsonPath("$.version").value("local"));
        }

        @Test
        @DisplayName("Liveness ve readiness UP döner")
        void probesAreUp() throws Exception {
            // when / then
            mockMvc.perform(get("/actuator/health/liveness"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
            mockMvc.perform(get("/actuator/health/readiness"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }

        @Test
        @DisplayName("Prometheus metrikleri yayınlanır")
        void prometheusEndpointIsExposed() throws Exception {
            // when / then
            mockMvc.perform(get("/actuator/prometheus"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @SpringBootTest(properties = {"DEMO_BROKEN=true", "APP_VERSION=a1b2c3d"})
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    @DisplayName("Bozuk sürüm (DEMO_BROKEN=true)")
    class BrokenVersion {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("Readiness DOWN (503), liveness UP kalır")
        void readinessIsDownButLivenessIsUp() throws Exception {
            // when / then
            mockMvc.perform(get("/actuator/health/readiness"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("DOWN"));
            mockMvc.perform(get("/actuator/health/liveness"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }

        @Test
        @DisplayName("/api/info APP_VERSION değerini döner")
        void infoReturnsAppVersion() throws Exception {
            // when / then
            mockMvc.perform(get("/api/info"))
                    .andExpect(jsonPath("$.version").value("a1b2c3d"));
        }
    }
}
