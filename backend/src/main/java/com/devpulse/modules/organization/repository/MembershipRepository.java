package com.devpulse.modules.organization.repository;

import com.devpulse.modules.organization.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    List<Membership> findByUserId(UUID userId);

    List<Membership> findByOrganizationId(UUID organizationId);

    Optional<Membership> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    boolean existsByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    @Query("SELECT m FROM Membership m JOIN FETCH m.organization WHERE m.user.id = :userId")
    List<Membership> findAllByUserIdWithOrganization(@Param("userId") UUID userId);

    @Query("SELECT m FROM Membership m JOIN FETCH m.user WHERE m.organization.id = :organizationId")
    List<Membership> findAllByOrganizationIdWithUser(@Param("organizationId") UUID organizationId);
}

