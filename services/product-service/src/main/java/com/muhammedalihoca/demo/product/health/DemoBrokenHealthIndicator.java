package com.muhammedalihoca.demo.product.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

// "Bozuk sürüm" anahtarı: DEMO_BROKEN=true ise bu kontrol DOWN döner.
// application.yml'de readiness grubuna eklendiği için pod "hazır değil" olur, trafik almaz ve rollout takılır.
// Bean adı "demoBrokenHealthIndicator" -> health'te "demoBroken" olarak görünür.
@Component
public class DemoBrokenHealthIndicator implements HealthIndicator {

    private final boolean broken;

    public DemoBrokenHealthIndicator(@Value("${app.demo-broken}") boolean broken) {
        this.broken = broken;
    }

    @Override
    public Health health() {
        if (broken) {
            return Health.down()
                    .withDetail("reason", "DEMO_BROKEN=true: bu sürüm bilerek bozuk")
                    .build();
        }
        return Health.up().build();
    }
}
