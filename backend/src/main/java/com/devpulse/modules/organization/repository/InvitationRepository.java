package com.devpulse.modules.organization.repository;

import com.devpulse.modules.organization.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    Optional<Invitation> findByToken(String token);
    List<Invitation> findByOrganizationId(UUID organizationId);
    List<Invitation> findByOrganizationIdAndStatus(UUID organizationId, String status);
}

