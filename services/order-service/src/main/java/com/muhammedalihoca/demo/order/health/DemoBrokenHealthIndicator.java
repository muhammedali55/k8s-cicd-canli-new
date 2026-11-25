package com.muhammedalihoca.demo.order.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

// "Bozuk sürüm" anahtarı: DEMO_BROKEN=true ise readiness DOWN döner.
// Canlı demoda readiness probe'un bozuk sürümü trafikten uzak tuttuğunu göstermek için.
// Bean adı "demoBrokenHealthIndicator" → Actuator'da "demoBroken" adıyla görünür.
@Component
public class DemoBrokenHealthIndicator implements HealthIndicator {

    private final boolean broken;

    public DemoBrokenHealthIndicator(@Value("${app.demo-broken}") boolean broken) {
        this.broken = broken;
    }

    @Override
    public Health health() {
        if (broken) {
            return Health.down().withDetail("reason", "DEMO_BROKEN=true").build();
        }
        return Health.up().build();
    }
}
