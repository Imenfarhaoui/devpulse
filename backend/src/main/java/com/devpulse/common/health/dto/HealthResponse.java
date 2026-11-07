package com.devpulse.common.health.dto;

public record HealthResponse(
        String status,
        String application,
        String version
) {
}