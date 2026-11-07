package com.devpulse;

import com.devpulse.common.websocket.RealtimeBroadcastService;
import com.devpulse.modules.alert.service.AlertService;
import com.devpulse.modules.audit.service.AuditService;
import com.devpulse.modules.incident.entity.Incident;
import com.devpulse.modules.incident.entity.IncidentEvent;
import com.devpulse.modules.incident.repository.IncidentEventRepository;
import com.devpulse.modules.incident.repository.IncidentRepository;
import com.devpulse.modules.incident.service.IncidentService;
import com.devpulse.modules.service.entity.MonitoredService;
import com.devpulse.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentStateMachineTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentEventRepository incidentEventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AlertService alertService;

    @Mock
    private RealtimeBroadcastService realtimeBroadcastService;

    @Mock
    private AuditService auditService;

    private IncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(
                incidentRepository,
                incidentEventRepository,
                userRepository,
                alertService,
                realtimeBroadcastService,
                auditService
        );
    }

    @Test
    @DisplayName("Single failure should NOT trigger incident (threshold is 3)")
    void singleFailureShouldNotTriggerIncident() {
        UUID orgId = UUID.randomUUID();
        MonitoredService service = new MonitoredService(orgId, "API Gateway", "https://api.example.com", "GET", 200, 30, 5000, "PRODUCTION");
        service.setId(UUID.randomUUID());
        service.setFailureThreshold(3);

        when(incidentRepository.findFirstByServiceIdAndStatusInOrderByStartedAtDesc(eq(service.getId()), anyList()))
                .thenReturn(Optional.empty());

        incidentService.processCheckResult(service, false, "Connection timed out");

        assertEquals(1, service.getConsecutiveFailures());
        assertEquals(0, service.getConsecutiveSuccesses());
        assertEquals("DEGRADED", service.getCurrentStatus());
        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    @DisplayName("Three consecutive failures triggers an OPEN incident")
    void threeConsecutiveFailuresTriggersOpenIncident() {
        UUID orgId = UUID.randomUUID();
        MonitoredService service = new MonitoredService(orgId, "API Gateway", "https://api.example.com", "GET", 200, 30, 5000, "PRODUCTION");
        service.setId(UUID.randomUUID());
        service.setConsecutiveFailures(2); // Already 2 previous failures
        service.setFailureThreshold(3);

        when(incidentRepository.findFirstByServiceIdAndStatusInOrderByStartedAtDesc(eq(service.getId()), anyList()))
                .thenReturn(Optional.empty());
        when(incidentRepository.findByServiceIdOrderByStartedAtDesc(eq(service.getId())))
                .thenReturn(Collections.emptyList());

        ArgumentCaptor<Incident> incidentCaptor = ArgumentCaptor.forClass(Incident.class);
        when(incidentRepository.save(incidentCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        incidentService.processCheckResult(service, false, "HTTP 500 Internal Server Error");

        assertEquals(3, service.getConsecutiveFailures());
        assertEquals("DOWN", service.getCurrentStatus());

        Incident createdIncident = incidentCaptor.getValue();
        assertNotNull(createdIncident);
        assertEquals("OPEN", createdIncident.getStatus());
        assertEquals(service.getId(), createdIncident.getServiceId());

        verify(incidentEventRepository, times(1)).save(any(IncidentEvent.class));
        verify(alertService, times(1)).dispatchIncidentAlert(eq(createdIncident), eq(service), eq("TRIGGERED"));
        verify(realtimeBroadcastService, times(1)).broadcastIncidentUpdate(eq(orgId), any());
    }

    @Test
    @DisplayName("Two consecutive successes auto-resolve an active incident")
    void twoConsecutiveSuccessesAutoResolvesIncident() {
        UUID orgId = UUID.randomUUID();
        MonitoredService service = new MonitoredService(orgId, "Payment Service", "https://pay.example.com", "GET", 200, 30, 5000, "PRODUCTION");
        service.setId(UUID.randomUUID());
        service.setCurrentStatus("DOWN");
        service.setConsecutiveFailures(4);
        service.setConsecutiveSuccesses(1); // Already 1 success
        service.setRecoveryThreshold(2);

        Incident activeIncident = new Incident(orgId, service.getId(), "Service Failure", "Failures", "HIGH");
        activeIncident.setStatus("OPEN");

        when(incidentRepository.findFirstByServiceIdAndStatusInOrderByStartedAtDesc(eq(service.getId()), anyList()))
                .thenReturn(Optional.of(activeIncident));

        incidentService.processCheckResult(service, true, null);

        assertEquals(2, service.getConsecutiveSuccesses());
        assertEquals(0, service.getConsecutiveFailures());
        assertEquals("UP", service.getCurrentStatus());
        assertEquals("RESOLVED", activeIncident.getStatus());
        assertNotNull(activeIncident.getResolvedAt());

        verify(incidentRepository, times(1)).save(activeIncident);
        verify(incidentEventRepository, times(1)).save(any(IncidentEvent.class));
        verify(alertService, times(1)).dispatchIncidentAlert(eq(activeIncident), eq(service), eq("RESOLVED"));
    }
}

