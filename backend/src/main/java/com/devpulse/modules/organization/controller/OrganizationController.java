package com.devpulse.modules.organization.controller;

import com.devpulse.common.security.UserPrincipal;
import com.devpulse.modules.auth.dto.AuthResponse;
import com.devpulse.modules.auth.dto.CreateOrganizationRequest;
import com.devpulse.modules.auth.dto.InviteRequest;
import com.devpulse.modules.organization.entity.Invitation;
import com.devpulse.modules.organization.entity.Organization;
import com.devpulse.modules.organization.service.InvitationService;
import com.devpulse.modules.organization.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final InvitationService invitationService;

    public OrganizationController(OrganizationService organizationService, InvitationService invitationService) {
        this.organizationService = organizationService;
        this.invitationService = invitationService;
    }

    @PostMapping
    public ResponseEntity<Organization> createOrganization(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.ok(organizationService.createOrganization(principal.getId(), request.getName()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<AuthResponse.OrgMembershipDto>> getUserOrganizations(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(organizationService.getUserOrganizations(principal.getId()));
    }

    @GetMapping("/{orgId}")
    public ResponseEntity<Organization> getOrganization(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orgId) {
        return ResponseEntity.ok(organizationService.getOrganization(principal.getId(), orgId));
    }

    @GetMapping("/{orgId}/members")
    public ResponseEntity<List<OrganizationService.MemberDto>> getMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orgId) {
        return ResponseEntity.ok(organizationService.getOrganizationMembers(principal.getId(), orgId));
    }

    @PatchMapping("/{orgId}/members/{targetUserId}/role")
    public ResponseEntity<Void> updateMemberRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orgId,
            @PathVariable UUID targetUserId,
            @RequestBody Map<String, String> body) {
        String newRole = body.get("role");
        organizationService.updateMemberRole(principal.getId(), orgId, targetUserId, newRole);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{orgId}/members/{targetUserId}")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orgId,
            @PathVariable UUID targetUserId) {
        organizationService.removeMember(principal.getId(), orgId, targetUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orgId}/invitations")
    public ResponseEntity<Invitation> invite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orgId,
            @Valid @RequestBody InviteRequest request) {
        return ResponseEntity.ok(invitationService.createInvitation(principal.getId(), orgId, request));
    }

    @GetMapping("/{orgId}/invitations")
    public ResponseEntity<List<Invitation>> getInvitations(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orgId) {
        return ResponseEntity.ok(invitationService.getInvitations(principal.getId(), orgId));
    }

    @DeleteMapping("/{orgId}/invitations/{invitationId}")
    public ResponseEntity<Void> revokeInvitation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orgId,
            @PathVariable UUID invitationId) {
        invitationService.revokeInvitation(principal.getId(), orgId, invitationId);
        return ResponseEntity.noContent().build();
    }
}

