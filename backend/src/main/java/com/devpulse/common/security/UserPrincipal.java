package com.devpulse.common.security;

import com.devpulse.modules.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final String globalRole;
    private final UUID activeOrganizationId;
    private final String organizationRole;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UUID id, String email, String password, String firstName, String lastName,
                         String globalRole, UUID activeOrganizationId, String organizationRole) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.globalRole = globalRole;
        this.activeOrganizationId = activeOrganizationId;
        this.organizationRole = organizationRole;

        String effectiveRole = organizationRole != null ? "ROLE_" + organizationRole : "ROLE_" + globalRole;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(effectiveRole));
    }

    public static UserPrincipal create(User user, UUID activeOrgId, String orgRole) {
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getFirstName(),
                user.getLastName(),
                user.getGlobalRole(),
                activeOrgId,
                orgRole
        );
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getGlobalRole() {
        return globalRole;
    }

    public UUID getActiveOrganizationId() {
        return activeOrganizationId;
    }

    public String getOrganizationRole() {
        return organizationRole;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

