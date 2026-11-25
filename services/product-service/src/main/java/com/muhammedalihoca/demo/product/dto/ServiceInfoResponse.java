package com.muhammedalihoca.demo.product.dto;

// GET /api/info yanıtı: servis adı ve çalışan sürüm.
public record ServiceInfoResponse(String service, String version) {
}
