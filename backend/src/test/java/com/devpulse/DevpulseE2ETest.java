package com.devpulse;

import com.devpulse.modules.alert.entity.AlertRule;
import com.devpulse.modules.alert.entity.Notification;
import com.devpulse.modules.alert.repository.AlertRuleRepository;
import com.devpulse.modules.alert.repository.NotificationRepository;
import com.devpulse.modules.auth.dto.AuthResponse;
import com.devpulse.modules.auth.dto.InviteRequest;
import com.devpulse.modules.auth.dto.RegisterRequest;
import com.devpulse.modules.auth.service.AuthService;
import com.devpulse.modules.incident.entity.Incident;
import com.devpulse.modules.incident.entity.IncidentEvent;
import com.devpulse.modules.incident.repository.IncidentEventRepository;
import com.devpulse.modules.incident.repository.IncidentRepository;
import com.devpulse.modules.incident.service.IncidentService;
import com.devpulse.modules.organization.entity.Invitation;
import com.devpulse.modules.organization.service.InvitationService;
import com.devpulse.modules.service.entity.MonitoredService;
import com.devpulse.modules.service.repository.MonitoredServiceRepository;
import com.devpulse.modules.service.service.ServiceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DevpulseE2ETest {

    @Autowired
    private AuthService authService;

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private MonitoredServiceRepository serviceRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentEventRepository incidentEventRepository;

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private InvitationService invitationService;

    @Test
    @DisplayName("End-to-End Workflow: Auth -> Service Registration -> Failure Threshold (3 checks) -> Incident Creation -> Ack -> Recovery Threshold (2 checks) -> Auto-Resolve")
    void testCompleteMonitoringAndIncidentLifecycle() {
        String testEmail = "test_owner_" + UUID.randomUUID().toString().substring(0, 6) + "@company.com";
        AuthResponse ownerAuth = authService.register(new RegisterRequest(
                "John", "Doe", testEmail, "Password123!", "Initech Corp"
        ));

        UUID orgId = ownerAuth.getActiveOrganization().getId();
        UUID userId = ownerAuth.getUser().getId();
        assertEquals("ORG_OWNER", ownerAuth.getOrganizationRole());

        // 1. Create Alert Rule for Email notifications with 15 min cooldown
        AlertRule alertRule = alertRuleRepository.save(
                new AlertRule(orgId, null, "EMAIL", "sre@initech.com", 15)
        );
        assertNotNull(alertRule.getId());

        // 2. Register Monitored Service (3 failure threshold, 2 recovery threshold)
        MonitoredService service = serviceService.registerService(
                orgId, userId, testEmail,
                "Payment Webhook Listener",
                "https://api.initech.com/webhooks",
                "POST", 200, 15, 3000, "PRODUCTION"
        );
        assertNotNull(service.getId());
        assertEquals("PENDING", service.getCurrentStatus());
        assertEquals(3, service.getFailureThreshold());
        assertEquals(2, service.getRecoveryThreshold());

        // 3. First check fails -> Consecutive failures = 1, status = DEGRADED, no incident created
        incidentService.processCheckResult(service, false, "Connection refused: port 443");
        assertEquals(1, service.getConsecutiveFailures());
        assertEquals("DEGRADED", service.getCurrentStatus());
        assertEquals(0, incidentRepository.findByServiceIdOrderByStartedAtDesc(service.getId()).size());

        // 4. Second check fails -> Consecutive failures = 2, status = DEGRADED, no incident created
        incidentService.processCheckResult(service, false, "Connection refused: port 443");
        assertEquals(2, service.getConsecutiveFailures());
        assertEquals("DEGRADED", service.getCurrentStatus());
        assertEquals(0, incidentRepository.findByServiceIdOrderByStartedAtDesc(service.getId()).size());

        // 5. Third check fails -> Consecutive failures = 3 >= failureThreshold (3)
        // -> Triggers OPEN Incident! Status transitions to DOWN! Dispatches notification!
        incidentService.processCheckResult(service, false, "Connection refused: port 443");
        assertEquals(3, service.getConsecutiveFailures());
        assertEquals("DOWN", service.getCurrentStatus());

        List<Incident> incidents = incidentRepository.findByServiceIdOrderByStartedAtDesc(service.getId());
        assertEquals(1, incidents.size(), "Exactly 1 incident should be created after 3 consecutive failures");

        Incident incident = incidents.get(0);
        assertEquals("OPEN", incident.getStatus());
        assertEquals("HIGH", incident.getSeverity());
        assertTrue(incident.getTitle().contains("Payment Webhook Listener"));

        // Verify lifecycle event recorded
        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdWithUser(incident.getId());
        assertFalse(events.isEmpty());
        assertEquals("TRIGGERED", events.get(0).getEventType());

        // Verify notification was recorded in notifications table
        List<Notification> notifications = notificationRepository.findByIncidentIdOrderBySentAtDesc(incident.getId());
        assertFalse(notifications.isEmpty(), "Notification should be dispatched for new incident");
        assertEquals("sre@initech.com", notifications.get(0).getRecipient());

        // 6. Fourth check fails while incident is already open -> should NOT create duplicate incident
        incidentService.processCheckResult(service, false, "Connection refused: port 443");
        assertEquals(4, service.getConsecutiveFailures());
        assertEquals(1, incidentRepository.findByServiceIdOrderByStartedAtDesc(service.getId()).size(),
                "Should not create duplicate incidents for the same service outage");

        // 7. Incident Manager acknowledges incident
        incidentService.acknowledgeIncident(orgId, incident.getId(), userId, "SRE team on call investigating DNS");
        Incident ackedIncident = incidentRepository.findById(incident.getId()).orElseThrow();
        assertEquals("ACKNOWLEDGED", ackedIncident.getStatus());
        assertNotNull(ackedIncident.getAcknowledgedAt());

        // 8. Service recovery begins:
        // First successful check -> Consecutive successes = 1 (< recovery threshold 2), incident remains ACKNOWLEDGED
        incidentService.processCheckResult(service, true, null);
        assertEquals(1, service.getConsecutiveSuccesses());
        Incident intermediateIncident = incidentRepository.findById(incident.getId()).orElseThrow();
        assertEquals("ACKNOWLEDGED", intermediateIncident.getStatus());

        // Second successful check -> Consecutive successes = 2 >= recovery threshold (2)
        // -> Auto-resolves incident! Status transitions to UP!
        incidentService.processCheckResult(service, true, null);
        assertEquals(2, service.getConsecutiveSuccesses());
        assertEquals(0, service.getConsecutiveFailures());
        assertEquals("UP", service.getCurrentStatus());

        Incident resolvedIncident = incidentRepository.findById(incident.getId()).orElseThrow();
        assertEquals("RESOLVED", resolvedIncident.getStatus());
        assertNotNull(resolvedIncident.getResolvedAt());

        // Verify recovery lifecycle event recorded
        List<IncidentEvent> updatedEvents = incidentEventRepository.findByIncidentIdWithUser(incident.getId());
        boolean hasResolvedEvent = updatedEvents.stream().anyMatch(e -> "RESOLVED".equals(e.getEventType()));
        assertTrue(hasResolvedEvent, "Incident event history must record RESOLVED transition");

        // 9. Verify Invitation workflow for adding new team member
        Invitation invitation = invitationService.createInvitation(
                userId, orgId, new InviteRequest("developer@initech.com", "DEVELOPER")
        );
        assertNotNull(invitation.getToken());
        assertEquals("PENDING", invitation.getStatus());
        assertEquals("DEVELOPER", invitation.getRole());
    }
}

