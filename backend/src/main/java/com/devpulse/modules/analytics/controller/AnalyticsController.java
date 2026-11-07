package com.devpulse.modules.analytics.controller;

import com.devpulse.common.security.TenantContext;
import com.devpulse.common.security.UserPrincipal;
import com.devpulse.modules.analytics.service.AnalyticsService;
import com.devpulse.modules.organization.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final OrganizationService organizationService;

    public AnalyticsController(AnalyticsService analyticsService, OrganizationService organizationService) {
        this.analyticsService = analyticsService;
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

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview(@AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = resolveOrgId(principal);
        return ResponseEntity.ok(analyticsService.getOrganizationOverview(orgId));
    }

    @GetMapping("/services/{serviceId}")
    public ResponseEntity<Map<String, Object>> getServiceAnalytics(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID serviceId) {
        UUID orgId = resolveOrgId(principal);
        return ResponseEntity.ok(analyticsService.getServiceAnalytics(orgId, serviceId));
    }
}

