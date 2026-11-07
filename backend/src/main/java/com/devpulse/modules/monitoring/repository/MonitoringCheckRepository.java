package com.devpulse.modules.monitoring.repository;

import com.devpulse.modules.monitoring.entity.MonitoringCheck;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MonitoringCheckRepository extends JpaRepository<MonitoringCheck, UUID> {

    List<MonitoringCheck> findByServiceIdOrderByCheckedAtDesc(UUID serviceId, Pageable pageable);

    List<MonitoringCheck> findByOrganizationIdOrderByCheckedAtDesc(UUID organizationId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM MonitoringCheck c WHERE c.serviceId = :serviceId AND c.checkedAt >= :since")
    long countTotalChecksSince(@Param("serviceId") UUID serviceId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(c) FROM MonitoringCheck c WHERE c.serviceId = :serviceId AND c.success = true AND c.checkedAt >= :since")
    long countSuccessfulChecksSince(@Param("serviceId") UUID serviceId, @Param("since") LocalDateTime since);

    @Query("SELECT AVG(c.responseTimeMs) FROM MonitoringCheck c WHERE c.serviceId = :serviceId AND c.checkedAt >= :since")
    Double getAverageResponseTimeSince(@Param("serviceId") UUID serviceId, @Param("since") LocalDateTime since);

    @Query("SELECT c FROM MonitoringCheck c WHERE c.serviceId = :serviceId AND c.checkedAt >= :since ORDER BY c.checkedAt ASC")
    List<MonitoringCheck> getHistoricalChecksSince(@Param("serviceId") UUID serviceId, @Param("since") LocalDateTime since);

    long countByCheckedAtGreaterThanEqual(LocalDateTime since);
}

