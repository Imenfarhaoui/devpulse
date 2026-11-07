package com.devpulse.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateOrganizationRequest {

    @NotBlank(message = "Organization name is required")
    private String name;

    public CreateOrganizationRequest() {
    }

    public CreateOrganizationRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

