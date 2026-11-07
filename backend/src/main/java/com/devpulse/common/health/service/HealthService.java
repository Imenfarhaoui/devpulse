package com.devpulse.common.health.service;

import com.devpulse.common.health.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public HealthResponse checkHealth() {

        return new HealthResponse(
                "UP",
                "DevPulse",
                "0.0.1"
        );
    }
}