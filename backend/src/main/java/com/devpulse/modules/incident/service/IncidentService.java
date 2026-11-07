package com.devpulse.modules.incident.service;

import com.devpulse.common.websocket.RealtimeBroadcastService;
import com.devpulse.modules.alert.service.AlertService;
import com.devpulse.modules.audit.service.AuditService;
import com.devpulse.modules.incident.entity.Incident;
import com.devpulse.modules.incident.entity.IncidentEvent;
import com.devpulse.modules.incident.repository.IncidentEventRepository;
import com.devpulse.modules.incident.repository.IncidentRepository;
import com.devpulse.modules.service.entity.MonitoredService;
import com.devpulse.modules.user.entity.User;
import com.devpulse.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final UserRepository userRepository;
    private final AlertService alertService;
    private final RealtimeBroadcastService realtimeBroadcastService;
    private final AuditService auditService;

    public IncidentService(IncidentRepository incidentRepository,
                           IncidentEventRepository incidentEventRepository,
                           UserRepository userRepository,
                           AlertService alertService,
                           RealtimeBroadcastService realtimeBroadcastService,
                           AuditService auditService) {
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.userRepository = userRepository;
        this.alertService = alertService;
        this.realtimeBroadcastService = realtimeBroadcastService;
        this.auditService = auditService;
    }

    @Transactional
    public void processCheckResult(MonitoredService service, boolean isSuccess, String failureReason) {
        UUID orgId = service.getOrganizationId();
        List<String> activeStatuses = Arrays.asList("OPEN", "ACKNOWLEDGED", "REOPENED");
        Optional<Incident> activeIncidentOpt = incidentRepository
                .findFirstByServiceIdAndStatusInOrderByStartedAtDesc(service.getId(), activeStatuses);

        if (!isSuccess) {
            // Service check failed
            int consecutiveFailures = service.getConsecutiveFailures() + 1;
            service.setConsecutiveFailures(consecutiveFailures);
            service.setConsecutiveSuccesses(0);

            if (consecutiveFailures >= service.getFailureThreshold()) {
                if (activeIncidentOpt.isEmpty()) {
                    // Check if there was a recently resolved incident that should be reopened
                    Optional<Incident> lastIncidentOpt = incidentRepository
                            .findByServiceIdOrderByStartedAtDesc(service.getId())
                            .stream().findFirst();

                    Incident incident;
                    String eventType;
                    if (lastIncidentOpt.isPresent() && "RESOLVED".equals(lastIncidentOpt.get().getStatus())
                            && lastIncidentOpt.get().getResolvedAt() != null
                            && lastIncidentOpt.get().getResolvedAt().isAfter(LocalDateTime.now().minusHours(24))) {
                        // Reopen the existing incident
                        incident = lastIncidentOpt.get();
                        incident.setStatus("REOPENED");
                        incident.setResolvedAt(null);
                        eventType = "REOPENED";
                    } else {
                        // Create brand new incident
                        incident = new Incident(
                                orgId,
                                service.getId(),
                                "Service Failure: " + service.getName(),
                                failureReason != null ? failureReason : "Health check failed " + consecutiveFailures + " consecutive times",
                                consecutiveFailures >= 5 ? "CRITICAL" : "HIGH"
                        );
                        eventType = "TRIGGERED";
                    }
                    incident = incidentRepository.save(incident);

                    service.setCurrentStatus("DOWN");

                    // Record lifecycle event
                    IncidentEvent event = new IncidentEvent(
                            incident,
                            null,
                            eventType,
                            "Service reached " + consecutiveFailures + " consecutive failures. Error: " + failureReason
                    );
                    incidentEventRepository.save(event);

                    // Dispatch alerts
                    alertService.dispatchIncidentAlert(incident, service, eventType);

                    // Broadcast real-time WebSocket update
                    Map<String, Object> wsPayload = new HashMap<>();
                    wsPayload.put("type", "INCIDENT_" + eventType);
                    wsPayload.put("incidentId", incident.getId() != null ? incident.getId().toString() : "");
                    wsPayload.put("serviceId", service.getId() != null ? service.getId().toString() : "");
                    wsPayload.put("serviceName", service.getName());
                    wsPayload.put("status", incident.getStatus());
                    wsPayload.put("severity", incident.getSeverity());
                    wsPayload.put("startedAt", incident.getStartedAt() != null ? incident.getStartedAt().toString() : "");
                    realtimeBroadcastService.broadcastIncidentUpdate(orgId, wsPayload);

                    log.warn("Incident {} {} for service {} (Org: {})", incident.getId(), eventType, service.getName(), orgId);
                } else {
                    service.setCurrentStatus("DOWN");
                }
            } else {
                service.setCurrentStatus("DEGRADED");
            }
        } else {
            // Service check succeeded
            int consecutiveSuccesses = service.getConsecutiveSuccesses() + 1;
            service.setConsecutiveSuccesses(consecutiveSuccesses);

            if (activeIncidentOpt.isPresent()) {
                if (consecutiveSuccesses >= service.getRecoveryThreshold()) {
                    // Auto-resolve incident!
                    Incident activeIncident = activeIncidentOpt.get();
                    activeIncident.setStatus("RESOLVED");
                    activeIncident.setResolvedAt(LocalDateTime.now());
                    incidentRepository.save(activeIncident);

                    service.setConsecutiveFailures(0);
                    service.setCurrentStatus("UP");

                    IncidentEvent event = new IncidentEvent(
                            activeIncident,
                            null,
                            "RESOLVED",
                            "Auto-resolved after " + consecutiveSuccesses + " consecutive successful checks."
                    );
                    incidentEventRepository.save(event);

                    alertService.dispatchIncidentAlert(activeIncident, service, "RESOLVED");

                    Map<String, Object> wsPayload = new HashMap<>();
                    wsPayload.put("type", "INCIDENT_RESOLVED");
                    wsPayload.put("incidentId", activeIncident.getId() != null ? activeIncident.getId().toString() : "");
                    wsPayload.put("serviceId", service.getId() != null ? service.getId().toString() : "");
                    wsPayload.put("serviceName", service.getName());
                    wsPayload.put("status", "RESOLVED");
                    wsPayload.put("resolvedAt", activeIncident.getResolvedAt() != null ? activeIncident.getResolvedAt().toString() : "");
                    realtimeBroadcastService.broadcastIncidentUpdate(orgId, wsPayload);

                    log.info("Incident {} auto-resolved for service {} (Org: {})",
                            activeIncident.getId(), service.getName(), orgId);
                }
            } else {
                service.setConsecutiveFailures(0);
                service.setCurrentStatus("UP");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Incident> getIncidents(UUID orgId, String status) {
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            return incidentRepository.findByOrganizationIdAndStatusOrderByStartedAtDesc(orgId, status.toUpperCase());
        }
        return incidentRepository.findByOrganizationIdOrderByStartedAtDesc(orgId);
    }

    @Transactional(readOnly = true)
    public Incident getIncident(UUID orgId, UUID incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        if (!incident.getOrganizationId().equals(orgId)) {
            throw new IllegalArgumentException("Incident does not belong to this organization");
        }
        return incident;
    }

    @Transactional(readOnly = true)
    public List<IncidentEvent> getIncidentEvents(UUID orgId, UUID incidentId) {
        getIncident(orgId, incidentId);
        return incidentEventRepository.findByIncidentIdWithUser(incidentId);
    }

    @Transactional
    public Incident acknowledgeIncident(UUID orgId, UUID incidentId, UUID userId, String comment) {
        Incident incident = getIncident(orgId, incidentId);
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        incident.setStatus("ACKNOWLEDGED");
        incident.setAcknowledgedAt(LocalDateTime.now());
        if (user != null && incident.getAssignedTo() == null) {
            incident.setAssignedTo(user);
        }
        incidentRepository.save(incident);

        String note = comment != null && !comment.isBlank() ? comment : "Incident acknowledged by " + (user != null ? user.getFirstName() : "user");
        IncidentEvent event = new IncidentEvent(incident, user, "ACKNOWLEDGED", note);
        incidentEventRepository.save(event);

        auditService.log(orgId, userId, user != null ? user.getEmail() : null, "INCIDENT_ACKNOWLEDGED", "INCIDENT", incidentId.toString(), note);
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "INCIDENT_ACKNOWLEDGED");
        wsPayload.put("incidentId", incident.getId() != null ? incident.getId().toString() : "");
        wsPayload.put("status", "ACKNOWLEDGED");
        realtimeBroadcastService.broadcastIncidentUpdate(orgId, wsPayload);

        return incident;
    }

    @Transactional
    public Incident resolveIncident(UUID orgId, UUID incidentId, UUID userId, String resolutionNotes) {
        Incident incident = getIncident(orgId, incidentId);
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        incident.setStatus("RESOLVED");
        incident.setResolvedAt(LocalDateTime.now());
        incidentRepository.save(incident);

        String note = resolutionNotes != null && !resolutionNotes.isBlank() ? resolutionNotes : "Incident manually marked as resolved.";
        IncidentEvent event = new IncidentEvent(incident, user, "RESOLVED", note);
        incidentEventRepository.save(event);

        auditService.log(orgId, userId, user != null ? user.getEmail() : null, "INCIDENT_RESOLVED", "INCIDENT", incidentId.toString(), note);
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "INCIDENT_RESOLVED");
        wsPayload.put("incidentId", incident.getId() != null ? incident.getId().toString() : "");
        wsPayload.put("status", "RESOLVED");
        realtimeBroadcastService.broadcastIncidentUpdate(orgId, wsPayload);

        return incident;
    }

    @Transactional
    public Incident reopenIncident(UUID orgId, UUID incidentId, UUID userId, String reason) {
        Incident incident = getIncident(orgId, incidentId);
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        incident.setStatus("REOPENED");
        incident.setResolvedAt(null);
        incidentRepository.save(incident);

        String note = reason != null && !reason.isBlank() ? reason : "Incident reopened.";
        IncidentEvent event = new IncidentEvent(incident, user, "REOPENED", note);
        incidentEventRepository.save(event);

        auditService.log(orgId, userId, user != null ? user.getEmail() : null, "INCIDENT_REOPENED", "INCIDENT", incidentId.toString(), note);
        Map<String, Object> reopenPayload = new HashMap<>();
        reopenPayload.put("type", "INCIDENT_REOPENED");
        reopenPayload.put("incidentId", incident.getId() != null ? incident.getId().toString() : "");
        reopenPayload.put("status", "REOPENED");
        realtimeBroadcastService.broadcastIncidentUpdate(orgId, reopenPayload);

        return incident;
    }

    @Transactional
    public Incident assignIncident(UUID orgId, UUID incidentId, UUID callerId, UUID targetUserId) {
        Incident incident = getIncident(orgId, incidentId);
        User caller = callerId != null ? userRepository.findById(callerId).orElse(null) : null;
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        incident.setAssignedTo(targetUser);
        incidentRepository.save(incident);

        String note = "Assigned to " + targetUser.getFirstName() + " " + targetUser.getLastName();
        IncidentEvent event = new IncidentEvent(incident, caller, "ASSIGNED", note);
        incidentEventRepository.save(event);

        auditService.log(orgId, callerId, caller != null ? caller.getEmail() : null, "INCIDENT_ASSIGNED", "INCIDENT", incidentId.toString(), note);
        return incident;
    }

    @Transactional
    public IncidentEvent addComment(UUID orgId, UUID incidentId, UUID userId, String comment) {
        Incident incident = getIncident(orgId, incidentId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        IncidentEvent event = new IncidentEvent(incident, user, "COMMENT", comment);
        return incidentEventRepository.save(event);
    }
}

