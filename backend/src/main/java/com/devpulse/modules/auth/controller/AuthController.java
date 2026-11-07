package com.devpulse.modules.auth.controller;

import com.devpulse.common.security.UserPrincipal;
import com.devpulse.modules.auth.dto.*;
import com.devpulse.modules.auth.service.AuthService;
import com.devpulse.modules.organization.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final InvitationService invitationService;

    public AuthController(AuthService authService, InvitationService invitationService) {
        this.authService = authService;
        this.invitationService = invitationService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/switch-org/{orgId}")
    public ResponseEntity<AuthResponse> switchOrg(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orgId) {
        return ResponseEntity.ok(authService.switchOrganization(principal.getId(), orgId));
    }

    @PostMapping("/accept-invite")
    public ResponseEntity<String> acceptInvite(@Valid @RequestBody AcceptInviteRequest request) {
        invitationService.acceptInvitation(request);
        return ResponseEntity.ok("Invitation accepted successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<UserPrincipal> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(principal);
    }
}

