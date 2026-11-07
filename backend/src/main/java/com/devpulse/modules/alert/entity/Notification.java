package com.devpulse.modules.alert.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_org", columnList = "organization_id, sent_at DESC")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(name = "alert_rule_id")
    private UUID alertRuleId;

    @Column(nullable = false, length = 255)
    private String recipient;

    @Column(nullable = false, length = 50)
    private String channel;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 50)
    private String status = "SENT"; // SENT, FAILED

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        this.sentAt = LocalDateTime.now();
    }

    public Notification() {
    }

    public Notification(UUID organizationId, UUID incidentId, UUID alertRuleId, String recipient,
                        String channel, String title, String message, String status) {
        this.organizationId = organizationId;
        this.incidentId = incidentId;
        this.alertRuleId = alertRuleId;
        this.recipient = recipient;
        this.channel = channel;
        this.title = title;
        this.message = message;
        this.status = status != null ? status : "SENT";
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

    public UUID getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(UUID incidentId) {
        this.incidentId = incidentId;
    }

    public UUID getAlertRuleId() {
        return alertRuleId;
    }

    public void setAlertRuleId(UUID alertRuleId) {
        this.alertRuleId = alertRuleId;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}

