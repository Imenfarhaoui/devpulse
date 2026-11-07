package com.devpulse.modules.alert.repository;

import com.devpulse.modules.alert.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByOrganizationIdOrderBySentAtDesc(UUID organizationId, Pageable pageable);

    List<Notification> findByIncidentIdOrderBySentAtDesc(UUID incidentId);
}

