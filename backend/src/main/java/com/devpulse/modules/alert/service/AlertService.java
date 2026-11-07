package com.devpulse.modules.alert.service;

import com.devpulse.modules.alert.entity.AlertRule;
import com.devpulse.modules.alert.entity.Notification;
import com.devpulse.modules.alert.repository.AlertRuleRepository;
import com.devpulse.modules.alert.repository.NotificationRepository;
import com.devpulse.modules.incident.entity.Incident;
import com.devpulse.modules.service.entity.MonitoredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRuleRepository alertRuleRepository;
    private final NotificationRepository notificationRepository;

    public AlertService(AlertRuleRepository alertRuleRepository,
                        NotificationRepository notificationRepository) {
        this.alertRuleRepository = alertRuleRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public AlertRule createAlertRule(UUID orgId, UUID serviceId, String channel, String target, int cooldownMinutes) {
        AlertRule rule = new AlertRule(orgId, serviceId, channel, target, cooldownMinutes);
        return alertRuleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<AlertRule> getAlertRules(UUID orgId) {
        return alertRuleRepository.findByOrganizationId(orgId);
    }

    @Transactional
    public void deleteAlertRule(UUID orgId, UUID ruleId) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Alert rule not found"));
        if (!rule.getOrganizationId().equals(orgId)) {
            throw new IllegalArgumentException("Rule does not belong to this organization");
        }
        alertRuleRepository.delete(rule);
    }

    @Transactional
    public void toggleAlertRule(UUID orgId, UUID ruleId) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Alert rule not found"));
        if (!rule.getOrganizationId().equals(orgId)) {
            throw new IllegalArgumentException("Rule does not belong to this organization");
        }
        rule.setActive(!rule.isActive());
        alertRuleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotifications(UUID orgId, int limit) {
        return notificationRepository.findByOrganizationIdOrderBySentAtDesc(
                orgId, PageRequest.of(0, Math.min(limit, 100)));
    }

    @Transactional
    public void dispatchIncidentAlert(Incident incident, MonitoredService service, String eventType) {
        UUID orgId = incident.getOrganizationId();
        List<AlertRule> rules = new ArrayList<>();
        rules.addAll(alertRuleRepository.findByOrganizationIdAndServiceIdAndActiveTrue(orgId, service.getId()));
        rules.addAll(alertRuleRepository.findByOrganizationIdAndServiceIdIsNullAndActiveTrue(orgId));

        LocalDateTime now = LocalDateTime.now();

        for (AlertRule rule : rules) {
            // Check cooldown period
            if (rule.getLastTriggeredAt() != null) {
                LocalDateTime cooldownUntil = rule.getLastTriggeredAt().plusMinutes(rule.getCooldownMinutes());
                if (now.isBefore(cooldownUntil)) {
                    log.info("Alert rule {} is in cooldown until {}. Skipping notification.", rule.getId(), cooldownUntil);
                    continue;
                }
            }

            String title;
            String message;

            if ("TRIGGERED".equalsIgnoreCase(eventType) || "REOPENED".equalsIgnoreCase(eventType)) {
                title = String.format("[DevPulse Alert] %s is DOWN!", service.getName());
                message = String.format("Service '%s' (%s) failed health check. Incident '%s' triggered. Severity: %s. Started: %s",
                        service.getName(), service.getUrl(), incident.getTitle(), incident.getSeverity(), incident.getStartedAt());
            } else if ("RESOLVED".equalsIgnoreCase(eventType)) {
                title = String.format("[DevPulse Recovery] %s has RECOVERED", service.getName());
                message = String.format("Service '%s' (%s) passed consecutive recovery checks. Incident '%s' is now RESOLVED at %s.",
                        service.getName(), service.getUrl(), incident.getTitle(), incident.getResolvedAt());
            } else {
                title = String.format("[DevPulse Incident Update] %s: %s", service.getName(), eventType);
                message = String.format("Incident '%s' for service '%s' transitioned to %s.",
                        incident.getTitle(), service.getName(), eventType);
            }

            Notification notification = new Notification(
                    orgId,
                    incident.getId(),
                    rule.getId(),
                    rule.getTarget(),
                    rule.getChannel(),
                    title,
                    message,
                    "SENT"
            );
            notificationRepository.save(notification);

            rule.setLastTriggeredAt(now);
            alertRuleRepository.save(rule);

            log.info("Dispatched notification via {} to {} for incident {}",
                    rule.getChannel(), rule.getTarget(), incident.getId());
        }
    }
}

