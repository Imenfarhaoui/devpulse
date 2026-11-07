package com.devpulse.common.health.controller;

import com.devpulse.common.health.dto.HealthResponse;
import com.devpulse.common.health.service.HealthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public ResponseEntity<HealthResponse> checkHealth() {

        HealthResponse response = healthService.checkHealth();

        return ResponseEntity.ok(response);
    }
}