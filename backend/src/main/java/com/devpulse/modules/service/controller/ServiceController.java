package com.devpulse.modules.service.controller;

import com.devpulse.common.security.TenantContext;
import com.devpulse.common.security.UserPrincipal;
import com.devpulse.modules.organization.service.OrganizationService;
import com.devpulse.modules.service.entity.MonitoredService;
import com.devpulse.modules.service.service.ServiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
public class ServiceController {

    private final ServiceService serviceService;
    private final OrganizationService organizationService;

    public ServiceController(ServiceService serviceService, OrganizationService organizationService) {
        this.serviceService = serviceService;
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

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'DEVELOPER', 'PLATFORM_ADMIN')")
    public ResponseEntity<MonitoredService> registerService(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        UUID orgId = resolveOrgId(principal);
        String name = (String) body.get("name");
        String url = (String) body.get("url");
        String method = (String) body.getOrDefault("httpMethod", "GET");
        int statusCode = body.get("expectedStatusCode") != null ? Integer.parseInt(body.get("expectedStatusCode").toString()) : 200;
        int interval = body.get("checkIntervalSeconds") != null ? Integer.parseInt(body.get("checkIntervalSeconds").toString()) : 30;
        int timeout = body.get("timeoutMs") != null ? Integer.parseInt(body.get("timeoutMs").toString()) : 5000;
        String env = (String) body.getOrDefault("environment", "PRODUCTION");

        MonitoredService service = serviceService.registerService(
                orgId, principal.getId(), principal.getUsername(),
                name, url, method, statusCode, interval, timeout, env);

        return ResponseEntity.ok(service);
    }

    @GetMapping
    public ResponseEntity<List<MonitoredService>> getServices(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String environment) {
        UUID orgId = resolveOrgId(principal);
        return ResponseEntity.ok(serviceService.getServices(orgId, environment));
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<MonitoredService> getService(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID serviceId) {
        UUID orgId = resolveOrgId(principal);
        return ResponseEntity.ok(serviceService.getService(orgId, serviceId));
    }

    @PutMapping("/{serviceId}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'DEVELOPER', 'PLATFORM_ADMIN')")
    public ResponseEntity<MonitoredService> updateService(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID serviceId,
            @RequestBody Map<String, Object> body) {
        UUID orgId = resolveOrgId(principal);
        String name = (String) body.get("name");
        String url = (String) body.get("url");
        String method = (String) body.get("httpMethod");
        int statusCode = body.get("expectedStatusCode") != null ? Integer.parseInt(body.get("expectedStatusCode").toString()) : 0;
        int interval = body.get("checkIntervalSeconds") != null ? Integer.parseInt(body.get("checkIntervalSeconds").toString()) : 0;
        int timeout = body.get("timeoutMs") != null ? Integer.parseInt(body.get("timeoutMs").toString()) : 0;
        String env = (String) body.get("environment");

        MonitoredService service = serviceService.updateService(
                orgId, serviceId, principal.getId(), principal.getUsername(),
                name, url, method, statusCode, interval, timeout, env);

        return ResponseEntity.ok(service);
    }

    @PatchMapping("/{serviceId}/toggle")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'DEVELOPER', 'PLATFORM_ADMIN')")
    public ResponseEntity<MonitoredService> toggleService(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID serviceId) {
        UUID orgId = resolveOrgId(principal);
        return ResponseEntity.ok(serviceService.toggleService(orgId, serviceId, principal.getId(), principal.getUsername()));
    }

    @DeleteMapping("/{serviceId}")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'DEVELOPER', 'PLATFORM_ADMIN')")
    public ResponseEntity<Void> deleteService(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID serviceId) {
        UUID orgId = resolveOrgId(principal);
        serviceService.deleteService(orgId, serviceId, principal.getId(), principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{serviceId}/check")
    public ResponseEntity<Void> triggerManualCheck(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID serviceId) {
        UUID orgId = resolveOrgId(principal);
        serviceService.triggerManualCheck(orgId, serviceId);
        return ResponseEntity.accepted().build();
    }
}

