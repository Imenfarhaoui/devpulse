package com.devpulse.modules.analytics.service;

import com.devpulse.modules.incident.repository.IncidentRepository;
import com.devpulse.modules.monitoring.entity.MonitoringCheck;
import com.devpulse.modules.monitoring.repository.MonitoringCheckRepository;
import com.devpulse.modules.service.entity.MonitoredService;
import com.devpulse.modules.service.repository.MonitoredServiceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final MonitoredServiceRepository serviceRepository;
    private final MonitoringCheckRepository checkRepository;
    private final IncidentRepository incidentRepository;

    public AnalyticsService(MonitoredServiceRepository serviceRepository,
                            MonitoringCheckRepository checkRepository,
                            IncidentRepository incidentRepository) {
        this.serviceRepository = serviceRepository;
        this.checkRepository = checkRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOrganizationOverview(UUID orgId) {
        List<MonitoredService> services = serviceRepository.findByOrganizationId(orgId);
        long totalServices = services.size();
        long upServices = services.stream().filter(s -> "UP".equalsIgnoreCase(s.getCurrentStatus())).count();
        long downServices = services.stream().filter(s -> "DOWN".equalsIgnoreCase(s.getCurrentStatus())).count();
        long degradedServices = services.stream().filter(s -> "DEGRADED".equalsIgnoreCase(s.getCurrentStatus())).count();

        long openIncidents = incidentRepository.countByOrganizationIdAndStatusIn(orgId, Arrays.asList("OPEN", "ACKNOWLEDGED", "REOPENED"));
        long monthlyIncidents = incidentRepository.countIncidentsSince(orgId, LocalDateTime.now().minusDays(30));

        // 24h org uptime calculation
        LocalDateTime since24h = LocalDateTime.now().minusHours(24);
        long totalChecks = 0;
        long successfulChecks = 0;
        double sumLatency = 0;
        int latencyCount = 0;

        for (MonitoredService service : services) {
            long total = checkRepository.countTotalChecksSince(service.getId(), since24h);
            long success = checkRepository.countSuccessfulChecksSince(service.getId(), since24h);
            totalChecks += total;
            successfulChecks += success;

            Double avgLatency = checkRepository.getAverageResponseTimeSince(service.getId(), since24h);
            if (avgLatency != null) {
                sumLatency += avgLatency;
                latencyCount++;
            }
        }

        double uptime24h = totalChecks > 0 ? (double) successfulChecks / totalChecks * 100.0 : 100.0;
        double avgOrgLatency = latencyCount > 0 ? sumLatency / latencyCount : 0.0;

        Map<String, Object> result = new HashMap<>();
        result.put("totalServices", totalServices);
        result.put("upServices", upServices);
        result.put("downServices", downServices);
        result.put("degradedServices", degradedServices);
        result.put("openIncidents", openIncidents);
        result.put("monthlyIncidents", monthlyIncidents);
        result.put("uptime24h", Math.round(uptime24h * 100.0) / 100.0);
        result.put("avgLatencyMs", Math.round(avgOrgLatency));
        result.put("slaTarget", 99.9);
        result.put("slaCompliant", uptime24h >= 99.9);

        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getServiceAnalytics(UUID orgId, UUID serviceId) {
        MonitoredService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
        if (!service.getOrganizationId().equals(orgId)) {
            throw new IllegalArgumentException("Service does not belong to this organization");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since24h = now.minusHours(24);
        LocalDateTime since7d = now.minusDays(7);
        LocalDateTime since30d = now.minusDays(30);

        long total24h = checkRepository.countTotalChecksSince(serviceId, since24h);
        long success24h = checkRepository.countSuccessfulChecksSince(serviceId, since24h);
        double uptime24h = total24h > 0 ? (double) success24h / total24h * 100.0 : 100.0;

        long total7d = checkRepository.countTotalChecksSince(serviceId, since7d);
        long success7d = checkRepository.countSuccessfulChecksSince(serviceId, since7d);
        double uptime7d = total7d > 0 ? (double) success7d / total7d * 100.0 : 100.0;

        long total30d = checkRepository.countTotalChecksSince(serviceId, since30d);
        long success30d = checkRepository.countSuccessfulChecksSince(serviceId, since30d);
        double uptime30d = total30d > 0 ? (double) success30d / total30d * 100.0 : 100.0;

        Double avgLatency24h = checkRepository.getAverageResponseTimeSince(serviceId, since24h);
        long incidents30d = incidentRepository.countServiceIncidentsSince(serviceId, since30d);

        // Recent 50 checks for charting
        List<MonitoringCheck> recentChecks = checkRepository.findByServiceIdOrderByCheckedAtDesc(serviceId, PageRequest.of(0, 50));
        List<Map<String, Object>> chartPoints = recentChecks.stream()
                .sorted(Comparator.comparing(MonitoringCheck::getCheckedAt))
                .map(c -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("timestamp", c.getCheckedAt());
                    point.put("responseTimeMs", c.getResponseTimeMs());
                    point.put("success", c.isSuccess());
                    point.put("status", c.getHttpStatus());
                    return point;
                }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("serviceId", service.getId());
        response.put("serviceName", service.getName());
        response.put("environment", service.getEnvironment());
        response.put("currentStatus", service.getCurrentStatus());
        response.put("lastResponseTimeMs", service.getLastResponseTimeMs());
        response.put("uptime24h", Math.round(uptime24h * 100.0) / 100.0);
        response.put("uptime7d", Math.round(uptime7d * 100.0) / 100.0);
        response.put("uptime30d", Math.round(uptime30d * 100.0) / 100.0);
        response.put("avgLatency24h", avgLatency24h != null ? Math.round(avgLatency24h) : 0);
        response.put("incidents30d", incidents30d);
        response.put("errorRate24h", total24h > 0 ? Math.round((double)(total24h - success24h) / total24h * 1000.0) / 10.0 : 0.0);
        response.put("recentChecks", chartPoints);
        response.put("slaTarget", 99.9);
        response.put("slaCompliant", uptime30d >= 99.9);

        return response;
    }
}

