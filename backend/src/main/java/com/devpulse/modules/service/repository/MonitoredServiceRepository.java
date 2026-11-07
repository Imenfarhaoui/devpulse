package com.devpulse.modules.service.repository;

import com.devpulse.modules.service.entity.MonitoredService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonitoredServiceRepository extends JpaRepository<MonitoredService, UUID> {

    List<MonitoredService> findByOrganizationId(UUID organizationId);

    List<MonitoredService> findByOrganizationIdAndEnvironment(UUID organizationId, String environment);

    Optional<MonitoredService> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<MonitoredService> findByActiveTrue();

    long countByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndCurrentStatus(UUID organizationId, String currentStatus);
}

