package com.muhammedalihoca.demo.order.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// DEMO_BROKEN=true iken readiness'in DOWN (503), liveness'in ise UP kaldığını doğrular.
// Böylece Kubernetes pod'u yeniden başlatmaz, yalnızca trafikten çıkarır.
@SpringBootTest(properties = "app.demo-broken=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoBrokenReadinessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("DEMO_BROKEN=true ise readiness DOWN, liveness UP döner")
    void bozukSurum() throws Exception {
        // given: app.demo-broken=true

        // when / then
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /api/info servis adını ve varsayılan 'local' sürümünü döner")
    void info() throws Exception {
        // given: APP_VERSION tanımlı değil

        // when / then
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("order-service"))
                .andExpect(jsonPath("$.version").value("local"));
    }
}
