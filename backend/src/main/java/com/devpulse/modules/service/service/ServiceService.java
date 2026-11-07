package com.devpulse.modules.service.service;

import com.devpulse.modules.audit.service.AuditService;
import com.devpulse.modules.monitoring.service.MonitoringEngineService;
import com.devpulse.modules.service.entity.MonitoredService;
import com.devpulse.modules.service.repository.MonitoredServiceRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ServiceService {

    private final MonitoredServiceRepository serviceRepository;
    private final MonitoringEngineService monitoringEngineService;
    private final AuditService auditService;

    public ServiceService(MonitoredServiceRepository serviceRepository,
                          MonitoringEngineService monitoringEngineService,
                          AuditService auditService) {
        this.serviceRepository = serviceRepository;
        this.monitoringEngineService = monitoringEngineService;
        this.auditService = auditService;
    }

    @Transactional
    public MonitoredService registerService(UUID orgId, UUID userId, String userEmail,
                                           String name, String url, String httpMethod,
                                           int expectedStatusCode, int checkIntervalSeconds,
                                           int timeoutMs, String environment) {
        MonitoredService service = new MonitoredService(
                orgId,
                name.trim(),
                url.trim(),
                httpMethod != null ? httpMethod.trim().toUpperCase() : "GET",
                expectedStatusCode,
                checkIntervalSeconds,
                timeoutMs,
                environment != null ? environment.trim().toUpperCase() : "PRODUCTION"
        );
        service = serviceRepository.save(service);

        auditService.log(orgId, userId, userEmail, "SERVICE_REGISTERED", "SERVICE", service.getId().toString(),
                "Registered service: " + service.getName() + " (" + service.getUrl() + ")");

        // Trigger immediate initial check
        monitoringEngineService.triggerCheckNow(service.getId());

        return service;
    }

    @Transactional(readOnly = true)
    public List<MonitoredService> getServices(UUID orgId, String environment) {
        if (environment != null && !environment.isBlank() && !"ALL".equalsIgnoreCase(environment)) {
            return serviceRepository.findByOrganizationIdAndEnvironment(orgId, environment.toUpperCase());
        }
        return serviceRepository.findByOrganizationId(orgId);
    }

    @Transactional(readOnly = true)
    public MonitoredService getService(UUID orgId, UUID serviceId) {
        MonitoredService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
        if (!service.getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("Service does not belong to this organization");
        }
        return service;
    }

    @Transactional
    public MonitoredService updateService(UUID orgId, UUID serviceId, UUID userId, String userEmail,
                                          String name, String url, String httpMethod,
                                          int expectedStatusCode, int checkIntervalSeconds,
                                          int timeoutMs, String environment) {
        MonitoredService service = getService(orgId, serviceId);

        if (name != null && !name.isBlank()) service.setName(name.trim());
        if (url != null && !url.isBlank()) service.setUrl(url.trim());
        if (httpMethod != null && !httpMethod.isBlank()) service.setHttpMethod(httpMethod.trim().toUpperCase());
        if (expectedStatusCode > 0) service.setExpectedStatusCode(expectedStatusCode);
        if (checkIntervalSeconds > 0) service.setCheckIntervalSeconds(checkIntervalSeconds);
        if (timeoutMs > 0) service.setTimeoutMs(timeoutMs);
        if (environment != null && !environment.isBlank()) service.setEnvironment(environment.trim().toUpperCase());

        service = serviceRepository.save(service);

        auditService.log(orgId, userId, userEmail, "SERVICE_UPDATED", "SERVICE", service.getId().toString(),
                "Updated service config: " + service.getName());

        return service;
    }

    @Transactional
    public MonitoredService toggleService(UUID orgId, UUID serviceId, UUID userId, String userEmail) {
        MonitoredService service = getService(orgId, serviceId);
        service.setActive(!service.isActive());
        if (!service.isActive()) {
            service.setCurrentStatus("PAUSED");
        } else {
            service.setCurrentStatus("PENDING");
            monitoringEngineService.triggerCheckNow(service.getId());
        }
        service = serviceRepository.save(service);

        auditService.log(orgId, userId, userEmail, "SERVICE_TOGGLE", "SERVICE", service.getId().toString(),
                "Toggled service active: " + service.isActive());

        return service;
    }

    @Transactional
    public void deleteService(UUID orgId, UUID serviceId, UUID userId, String userEmail) {
        MonitoredService service = getService(orgId, serviceId);
        serviceRepository.delete(service);

        auditService.log(orgId, userId, userEmail, "SERVICE_DELETED", "SERVICE", serviceId.toString(),
                "Deleted service: " + service.getName());
    }

    public void triggerManualCheck(UUID orgId, UUID serviceId) {
        getService(orgId, serviceId);
        monitoringEngineService.triggerCheckNow(serviceId);
    }
}

