package com.devpulse.modules.auth.dto;

import java.util.List;
import java.util.UUID;

public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private UserDto user;
    private OrgDto activeOrganization;
    private String organizationRole;
    private List<OrgMembershipDto> userOrganizations;

    public AuthResponse() {
    }

    public AuthResponse(String accessToken, String refreshToken, UserDto user, OrgDto activeOrganization,
                        String organizationRole, List<OrgMembershipDto> userOrganizations) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
        this.activeOrganization = activeOrganization;
        this.organizationRole = organizationRole;
        this.userOrganizations = userOrganizations;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public OrgDto getActiveOrganization() {
        return activeOrganization;
    }

    public void setActiveOrganization(OrgDto activeOrganization) {
        this.activeOrganization = activeOrganization;
    }

    public String getOrganizationRole() {
        return organizationRole;
    }

    public void setOrganizationRole(String organizationRole) {
        this.organizationRole = organizationRole;
    }

    public List<OrgMembershipDto> getUserOrganizations() {
        return userOrganizations;
    }

    public void setUserOrganizations(List<OrgMembershipDto> userOrganizations) {
        this.userOrganizations = userOrganizations;
    }

    public static class UserDto {
        private UUID id;
        private String email;
        private String firstName;
        private String lastName;
        private String globalRole;

        public UserDto() {}

        public UserDto(UUID id, String email, String firstName, String lastName, String globalRole) {
            this.id = id;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.globalRole = globalRole;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getGlobalRole() { return globalRole; }
        public void setGlobalRole(String globalRole) { this.globalRole = globalRole; }
    }

    public static class OrgDto {
        private UUID id;
        private String name;
        private String slug;
        private String plan;

        public OrgDto() {}

        public OrgDto(UUID id, String name, String slug, String plan) {
            this.id = id;
            this.name = name;
            this.slug = slug;
            this.plan = plan;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public String getPlan() { return plan; }
        public void setPlan(String plan) { this.plan = plan; }
    }

    public static class OrgMembershipDto {
        private UUID organizationId;
        private String organizationName;
        private String slug;
        private String role;

        public OrgMembershipDto() {}

        public OrgMembershipDto(UUID organizationId, String organizationName, String slug, String role) {
            this.organizationId = organizationId;
            this.organizationName = organizationName;
            this.slug = slug;
            this.role = role;
        }

        public UUID getOrganizationId() { return organizationId; }
        public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
        public String getOrganizationName() { return organizationName; }
        public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}

