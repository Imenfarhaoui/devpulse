package com.devpulse.modules.alert.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alert_rules", indexes = {
    @Index(name = "idx_alert_rules_org", columnList = "organization_id")
})
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "service_id")
    private UUID serviceId; // null = all services in org

    @Column(nullable = false, length = 50)
    private String channel = "EMAIL"; // EMAIL, WEBHOOK

    @Column(nullable = false, length = 255)
    private String target; // email or webhook URL

    @Column(name = "cooldown_minutes", nullable = false)
    private int cooldownMinutes = 15;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public AlertRule() {
    }

    public AlertRule(UUID organizationId, UUID serviceId, String channel, String target, int cooldownMinutes) {
        this.organizationId = organizationId;
        this.serviceId = serviceId;
        this.channel = channel != null ? channel : "EMAIL";
        this.target = target;
        this.cooldownMinutes = cooldownMinutes > 0 ? cooldownMinutes : 15;
        this.active = true;
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

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getCooldownMinutes() {
        return cooldownMinutes;
    }

    public void setCooldownMinutes(int cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

