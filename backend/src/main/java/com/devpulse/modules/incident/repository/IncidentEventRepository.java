package com.devpulse.modules.incident.repository;

import com.devpulse.modules.incident.entity.IncidentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface IncidentEventRepository extends JpaRepository<IncidentEvent, UUID> {

    @Query("SELECT e FROM IncidentEvent e LEFT JOIN FETCH e.user WHERE e.incident.id = :incidentId ORDER BY e.createdAt ASC")
    List<IncidentEvent> findByIncidentIdWithUser(@Param("incidentId") UUID incidentId);
}

