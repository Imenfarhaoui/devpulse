package com.devpulse.modules.monitoring.service;

import com.devpulse.common.websocket.RealtimeBroadcastService;
import com.devpulse.modules.incident.service.IncidentService;
import com.devpulse.modules.monitoring.entity.MonitoringCheck;
import com.devpulse.modules.monitoring.repository.MonitoringCheckRepository;
import com.devpulse.modules.service.entity.MonitoredService;
import com.devpulse.modules.service.repository.MonitoredServiceRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@EnableScheduling
public class MonitoringEngineService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringEngineService.class);

    private final MonitoredServiceRepository serviceRepository;
    private final MonitoringCheckRepository checkRepository;
    private final HttpHealthCheckClient healthCheckClient;
    private final IncidentService incidentService;
    private final RealtimeBroadcastService realtimeBroadcastService;
    private final ExecutorService executorService;

    public MonitoringEngineService(MonitoredServiceRepository serviceRepository,
                                   MonitoringCheckRepository checkRepository,
                                   HttpHealthCheckClient healthCheckClient,
                                   IncidentService incidentService,
                                   RealtimeBroadcastService realtimeBroadcastService) {
        this.serviceRepository = serviceRepository;
        this.checkRepository = checkRepository;
        this.healthCheckClient = healthCheckClient;
        this.incidentService = incidentService;
        this.realtimeBroadcastService = realtimeBroadcastService;
        // Bounded thread pool for concurrent checks
        this.executorService = Executors.newFixedThreadPool(10);
    }

    @Scheduled(fixedDelay = 5000)
    public void scheduleChecks() {
        try {
            List<MonitoredService> activeServices = serviceRepository.findByActiveTrue();
            LocalDateTime now = LocalDateTime.now();

            for (MonitoredService service : activeServices) {
                boolean shouldCheck = service.getLastCheckedAt() == null ||
                        service.getLastCheckedAt().plusSeconds(service.getCheckIntervalSeconds()).isBefore(now);

                if (shouldCheck) {
                    executorService.submit(() -> executeSingleCheck(service.getId()));
                }
            }
        } catch (Exception e) {
            log.error("Error in scheduleChecks: {}", e.getMessage());
        }
    }

    public void triggerCheckNow(UUID serviceId) {
        executorService.submit(() -> executeSingleCheck(serviceId));
    }

    @Transactional
    public void executeSingleCheck(UUID serviceId) {
        try {
            MonitoredService service = serviceRepository.findById(serviceId).orElse(null);
            if (service == null || !service.isActive()) {
                return;
            }

            HttpHealthCheckClient.CheckExecutionResult result = healthCheckClient.executeCheck(
                    service.getUrl(),
                    service.getHttpMethod(),
                    service.getExpectedStatusCode(),
                    service.getTimeoutMs()
            );

            // Record check
            MonitoringCheck check = new MonitoringCheck(
                    service.getId(),
                    service.getOrganizationId(),
                    result.getHttpStatus(),
                    result.getResponseTimeMs(),
                    result.isSuccess(),
                    result.getErrorMessage()
            );
            checkRepository.save(check);

            // Update service status & telemetry
            service.setLastCheckedAt(LocalDateTime.now());
            service.setLastResponseTimeMs(result.getResponseTimeMs());

            // Process incident state machine
            incidentService.processCheckResult(service, result.isSuccess(), result.getErrorMessage());

            serviceRepository.save(service);

            // Broadcast real-time status update to dashboard
            Map<String, Object> wsPayload = new HashMap<>();
            wsPayload.put("type", "CHECK_COMPLETED");
            wsPayload.put("serviceId", service.getId() != null ? service.getId().toString() : "");
            wsPayload.put("serviceName", service.getName());
            wsPayload.put("status", service.getCurrentStatus());
            wsPayload.put("httpStatus", result.getHttpStatus() != null ? result.getHttpStatus() : 0);
            wsPayload.put("responseTimeMs", result.getResponseTimeMs());
            wsPayload.put("success", result.isSuccess());
            wsPayload.put("lastCheckedAt", service.getLastCheckedAt() != null ? service.getLastCheckedAt().toString() : "");
            realtimeBroadcastService.broadcastServiceCheck(service.getOrganizationId(), wsPayload);

        } catch (Exception e) {
            log.error("Failed to execute check for service {}: {}", serviceId, e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }
}

