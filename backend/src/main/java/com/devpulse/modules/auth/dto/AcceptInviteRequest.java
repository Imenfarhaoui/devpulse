package com.devpulse.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class AcceptInviteRequest {

    @NotBlank(message = "Invitation token is required")
    private String token;

    private String password;
    private String firstName;
    private String lastName;

    public AcceptInviteRequest() {
    }

    public AcceptInviteRequest(String token, String password, String firstName, String lastName) {
        this.token = token;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}

