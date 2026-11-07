package com.devpulse.modules.audit.repository;

import com.devpulse.modules.audit.entity.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    List<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

