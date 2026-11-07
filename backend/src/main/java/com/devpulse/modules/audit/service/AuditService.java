package com.devpulse.modules.audit.service;

import com.devpulse.modules.audit.entity.AuditLog;
import com.devpulse.modules.audit.repository.AuditLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(UUID orgId, UUID userId, String userEmail, String action,
                    String entityType, String entityId, String details) {
        try {
            AuditLog auditLog = new AuditLog(orgId, userId, userEmail, action, entityType, entityId, details, "127.0.0.1");
            auditLogRepository.save(auditLog);
        } catch (Exception ignored) {
            // Audit logging shouldn't crash primary flows
        }
    }
}

