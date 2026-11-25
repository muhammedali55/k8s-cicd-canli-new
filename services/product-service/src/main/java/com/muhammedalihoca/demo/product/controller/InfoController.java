package com.muhammedalihoca.demo.product.controller;

import com.muhammedalihoca.demo.product.dto.ServiceInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// GET /api/info: hangi sürümün çalıştığını gösterir (canlıda yeni sürüm geldi mi? sorusunun cevabı).
@RestController
public class InfoController {

    private static final String SERVICE_NAME = "product-service";

    private final String version;

    // app.version, application.yml'de ${APP_VERSION:local} olarak tanımlı.
    public InfoController(@Value("${app.version}") String version) {
        this.version = version;
    }

    @GetMapping("/api/info")
    public ServiceInfoResponse info() {
        return new ServiceInfoResponse(SERVICE_NAME, version);
    }
}
