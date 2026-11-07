package com.devpulse.modules.alert.repository;

import com.devpulse.modules.alert.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {

    List<AlertRule> findByOrganizationId(UUID organizationId);

    List<AlertRule> findByOrganizationIdAndActiveTrue(UUID organizationId);

    List<AlertRule> findByOrganizationIdAndServiceIdAndActiveTrue(UUID organizationId, UUID serviceId);

    List<AlertRule> findByOrganizationIdAndServiceIdIsNullAndActiveTrue(UUID organizationId);
}

