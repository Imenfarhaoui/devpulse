package com.devpulse.modules.admin.controller;

import com.devpulse.common.security.UserPrincipal;
import com.devpulse.modules.audit.entity.AuditLog;
import com.devpulse.modules.audit.repository.AuditLogRepository;
import com.devpulse.modules.monitoring.repository.MonitoringCheckRepository;
import com.devpulse.modules.organization.repository.OrganizationRepository;
import com.devpulse.modules.service.repository.MonitoredServiceRepository;
import com.devpulse.modules.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MonitoredServiceRepository serviceRepository;
    private final MonitoringCheckRepository checkRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminController(UserRepository userRepository,
                           OrganizationRepository organizationRepository,
                           MonitoredServiceRepository serviceRepository,
                           MonitoringCheckRepository checkRepository,
                           AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.serviceRepository = serviceRepository;
        this.checkRepository = checkRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats(@AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalOrganizations", organizationRepository.count());
        stats.put("totalServices", serviceRepository.count());
        stats.put("totalChecksToday", checkRepository.countByCheckedAtGreaterThanEqual(LocalDate.now().atStartOfDay()));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getGlobalAuditLogs(
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.min(limit, 200))));
    }

    @GetMapping("/audit-logs/org/{orgId}")
    public ResponseEntity<List<AuditLog>> getOrgAuditLogs(
            @PathVariable UUID orgId,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(auditLogRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId, PageRequest.of(0, Math.min(limit, 200))));
    }
}

