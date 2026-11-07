package com.devpulse.modules.monitoring.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "monitoring_checks", indexes = {
    @Index(name = "idx_checks_service_checked_at", columnList = "service_id, checked_at DESC"),
    @Index(name = "idx_checks_service_success", columnList = "service_id, success"),
    @Index(name = "idx_checks_org_checked_at", columnList = "organization_id, checked_at DESC")
})
public class MonitoringCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_time_ms", nullable = false)
    private long responseTimeMs;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "checked_at", nullable = false, updatable = false)
    private LocalDateTime checkedAt;

    @PrePersist
    protected void onCreate() {
        this.checkedAt = LocalDateTime.now();
    }

    public MonitoringCheck() {
    }

    public MonitoringCheck(UUID serviceId, UUID organizationId, Integer httpStatus,
                           long responseTimeMs, boolean success, String errorMessage) {
        this.serviceId = serviceId;
        this.organizationId = organizationId;
        this.httpStatus = httpStatus;
        this.responseTimeMs = responseTimeMs;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }
}

