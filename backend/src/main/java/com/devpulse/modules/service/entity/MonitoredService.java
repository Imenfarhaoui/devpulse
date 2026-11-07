package com.devpulse.modules.service.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "monitored_services")
public class MonitoredService {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 1024)
    private String url;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod = "GET";

    @Column(name = "expected_status_code", nullable = false)
    private int expectedStatusCode = 200;

    @Column(name = "check_interval_seconds", nullable = false)
    private int checkIntervalSeconds = 30;

    @Column(name = "timeout_ms", nullable = false)
    private int timeoutMs = 5000;

    @Column(nullable = false, length = 50)
    private String environment = "PRODUCTION"; // PRODUCTION, STAGING, DEVELOPMENT

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "current_status", nullable = false, length = 50)
    private String currentStatus = "PENDING"; // UP, DOWN, DEGRADED, PENDING

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures = 0;

    @Column(name = "consecutive_successes", nullable = false)
    private int consecutiveSuccesses = 0;

    @Column(name = "failure_threshold", nullable = false)
    private int failureThreshold = 3;

    @Column(name = "recovery_threshold", nullable = false)
    private int recoveryThreshold = 2;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "last_response_time_ms")
    private Long lastResponseTimeMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public MonitoredService() {
    }

    public MonitoredService(UUID organizationId, String name, String url, String httpMethod,
                            int expectedStatusCode, int checkIntervalSeconds, int timeoutMs,
                            String environment) {
        this.organizationId = organizationId;
        this.name = name;
        this.url = url;
        this.httpMethod = httpMethod != null ? httpMethod : "GET";
        this.expectedStatusCode = expectedStatusCode > 0 ? expectedStatusCode : 200;
        this.checkIntervalSeconds = checkIntervalSeconds > 0 ? checkIntervalSeconds : 30;
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : 5000;
        this.environment = environment != null ? environment : "PRODUCTION";
        this.active = true;
        this.currentStatus = "PENDING";
        this.consecutiveFailures = 0;
        this.consecutiveSuccesses = 0;
        this.failureThreshold = 3;
        this.recoveryThreshold = 2;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public int getExpectedStatusCode() {
        return expectedStatusCode;
    }

    public void setExpectedStatusCode(int expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    public int getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public void setCheckIntervalSeconds(int checkIntervalSeconds) {
        this.checkIntervalSeconds = checkIntervalSeconds;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public void setConsecutiveFailures(int consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    public int getConsecutiveSuccesses() {
        return consecutiveSuccesses;
    }

    public void setConsecutiveSuccesses(int consecutiveSuccesses) {
        this.consecutiveSuccesses = consecutiveSuccesses;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public int getRecoveryThreshold() {
        return recoveryThreshold;
    }

    public void setRecoveryThreshold(int recoveryThreshold) {
        this.recoveryThreshold = recoveryThreshold;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public Long getLastResponseTimeMs() {
        return lastResponseTimeMs;
    }

    public void setLastResponseTimeMs(Long lastResponseTimeMs) {
        this.lastResponseTimeMs = lastResponseTimeMs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

