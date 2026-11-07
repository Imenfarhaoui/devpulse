package com.devpulse.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    private UUID organizationId;

    public RefreshTokenRequest() {
    }

    public RefreshTokenRequest(String refreshToken, UUID organizationId) {
        this.refreshToken = refreshToken;
        this.organizationId = organizationId;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }
}

