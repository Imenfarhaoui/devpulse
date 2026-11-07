package com.devpulse.modules.alert.controller;

import com.devpulse.common.security.TenantContext;
import com.devpulse.common.security.UserPrincipal;
import com.devpulse.modules.alert.entity.AlertRule;
import com.devpulse.modules.alert.entity.Notification;
import com.devpulse.modules.alert.service.AlertService;
import com.devpulse.modules.organization.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;
    private final OrganizationService organizationService;

    public AlertController(AlertService alertService, OrganizationService organizationService) {
        this.alertService = alertService;
        this.organizationService = organizationService;
    }

    private UUID resolveOrgId(UserPrincipal principal) {
        UUID orgId = TenantContext.getCurrentOrgId();
        if (orgId == null) {
            orgId = principal.getActiveOrganizationId();
        }
        if (orgId == null) {
            throw new IllegalArgumentException("No active organization specified");
        }
        organizationService.validateUserInOrg(principal.getId(), orgId);
        return orgId;
    }

    @GetMapping("/rules")
    public ResponseEntity<List<AlertRule>> getAlertRules(@AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = resolveOrgId(principal);
        return ResponseEntity.ok(alertService.getAlertRules(orgId));
    }

    @PostMapping("/rules")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'DEVELOPER', 'INCIDENT_MANAGER', 'PLATFORM_ADMIN')")
    public ResponseEntity<AlertRule> createAlertRule(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        UUID orgId = resolveOrgId(principal);
        UUID serviceId = body.get("serviceId") != null && !body.get("serviceId").toString().isBlank() ?
                UUID.fromString(body.get("serviceId").toString()) : null;
        String channel = (String) body.getOrDefault("channel", "EMAIL");
        String target = (String) body.get("target");
        int cooldown = body.get("cooldownMinutes") != null ? Integer.parseInt(body.get("cooldownMinutes").toString()) : 15;

        return ResponseEntity.ok(alertService.createAlertRule(orgId, serviceId, channel, target, cooldown));
    }

    @PatchMapping("/rules/{ruleId}/toggle")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'DEVELOPER', 'INCIDENT_MANAGER', 'PLATFORM_ADMIN')")
    public ResponseEntity<Void> toggleAlertRule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID ruleId) {
        UUID orgId = resolveOrgId(principal);
        alertService.toggleAlertRule(orgId, ruleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/rules/{ruleId}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'DEVELOPER', 'PLATFORM_ADMIN')")
    public ResponseEntity<Void> deleteAlertRule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID ruleId) {
        UUID orgId = resolveOrgId(principal);
        alertService.deleteAlertRule(orgId, ruleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "50") int limit) {
        UUID orgId = resolveOrgId(principal);
        return ResponseEntity.ok(alertService.getNotifications(orgId, limit));
    }
}

