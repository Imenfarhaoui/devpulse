package com.devpulse.modules.incident.controller;

import com.devpulse.common.security.TenantContext;
import com.devpulse.common.security.UserPrincipal;
import com.devpulse.modules.incident.entity.Incident;
import com.devpulse.modules.incident.entity.IncidentEvent;
import com.devpulse.modules.incident.service.IncidentService;
import com.devpulse.modules.organization.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final OrganizationService organizationService;

    public IncidentController(IncidentService incidentService, OrganizationService organizationService) {
        this.incidentService = incidentService;
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

    @GetMapping
    public ResponseEntity<List<Incident>> getIncidents(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status) {
        UUID orgId = resolveOrgId(principal);
        return ResponseEntity.ok(incidentService.getIncidents(orgId, status));
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<Incident> getIncident(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID incidentId) {
        UUID orgId = resolveOrgId(principal);
        return ResponseEntity.ok(incidentService.getIncident(orgId, incidentId));
    }

    @GetMapping("/{incidentId}/events")
    public ResponseEntity<List<IncidentEvent>> getIncidentEvents(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID incidentId) {
        UUID orgId = resolveOrgId(principal);
        return ResponseEntity.ok(incidentService.getIncidentEvents(orgId, incidentId));
    }

    @PostMapping("/{incidentId}/acknowledge")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'INCIDENT_MANAGER', 'DEVELOPER', 'PLATFORM_ADMIN')")
    public ResponseEntity<Incident> acknowledgeIncident(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID incidentId,
            @RequestBody(required = false) Map<String, String> body) {
        UUID orgId = resolveOrgId(principal);
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(incidentService.acknowledgeIncident(orgId, incidentId, principal.getId(), comment));
    }

    @PostMapping("/{incidentId}/resolve")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'INCIDENT_MANAGER', 'DEVELOPER', 'PLATFORM_ADMIN')")
    public ResponseEntity<Incident> resolveIncident(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID incidentId,
            @RequestBody(required = false) Map<String, String> body) {
        UUID orgId = resolveOrgId(principal);
        String notes = body != null ? body.get("resolutionNotes") : null;
        return ResponseEntity.ok(incidentService.resolveIncident(orgId, incidentId, principal.getId(), notes));
    }

    @PostMapping("/{incidentId}/reopen")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'INCIDENT_MANAGER', 'DEVELOPER', 'PLATFORM_ADMIN')")
    public ResponseEntity<Incident> reopenIncident(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID incidentId,
            @RequestBody(required = false) Map<String, String> body) {
        UUID orgId = resolveOrgId(principal);
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(incidentService.reopenIncident(orgId, incidentId, principal.getId(), reason));
    }

    @PostMapping("/{incidentId}/assign")
    @PreAuthorize("hasAnyRole('ORG_OWNER', 'INCIDENT_MANAGER', 'PLATFORM_ADMIN')")
    public ResponseEntity<Incident> assignIncident(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID incidentId,
            @RequestBody Map<String, String> body) {
        UUID orgId = resolveOrgId(principal);
        UUID targetUserId = UUID.fromString(body.get("userId"));
        return ResponseEntity.ok(incidentService.assignIncident(orgId, incidentId, principal.getId(), targetUserId));
    }

    @PostMapping("/{incidentId}/comments")
    public ResponseEntity<IncidentEvent> addComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID incidentId,
            @RequestBody Map<String, String> body) {
        UUID orgId = resolveOrgId(principal);
        String comment = body.get("comment");
        return ResponseEntity.ok(incidentService.addComment(orgId, incidentId, principal.getId(), comment));
    }
}

