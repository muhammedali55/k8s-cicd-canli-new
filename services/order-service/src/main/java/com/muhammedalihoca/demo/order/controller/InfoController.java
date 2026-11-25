package com.muhammedalihoca.demo.order.controller;

import com.muhammedalihoca.demo.order.dto.InfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// GET /api/info: servis adını ve çalışan sürümü (APP_VERSION) döner.
// Canlı demoda yeni sürümün cluster'a geldiğini göstermek için kullanılır.
@RestController
public class InfoController {

    private final String version;

    public InfoController(@Value("${app.version}") String version) {
        this.version = version;
    }

    @GetMapping("/api/info")
    public InfoResponse info() {
        return new InfoResponse("order-service", version);
    }
}
