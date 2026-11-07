package com.devpulse.modules.incident.repository;

import com.devpulse.modules.incident.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    List<Incident> findByOrganizationIdOrderByStartedAtDesc(UUID organizationId);

    List<Incident> findByOrganizationIdAndStatusOrderByStartedAtDesc(UUID organizationId, String status);

    List<Incident> findByServiceIdOrderByStartedAtDesc(UUID serviceId);

    Optional<Incident> findFirstByServiceIdAndStatusInOrderByStartedAtDesc(UUID serviceId, List<String> statuses);

    long countByOrganizationIdAndStatusIn(UUID organizationId, List<String> statuses);

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.organizationId = :orgId AND i.startedAt >= :since")
    long countIncidentsSince(@Param("orgId") UUID orgId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.serviceId = :serviceId AND i.startedAt >= :since")
    long countServiceIncidentsSince(@Param("serviceId") UUID serviceId, @Param("since") LocalDateTime since);
}

