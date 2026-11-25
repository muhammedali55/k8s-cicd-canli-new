package com.muhammedalihoca.demo.order.dto;

// GET /api/info yanıtı: servis adı ve çalışan sürüm.
public record InfoResponse(String service, String version) {
}
