package com.devpulse.common.data;

import com.devpulse.modules.alert.entity.AlertRule;
import com.devpulse.modules.alert.repository.AlertRuleRepository;
import com.devpulse.modules.incident.entity.Incident;
import com.devpulse.modules.incident.entity.IncidentEvent;
import com.devpulse.modules.incident.repository.IncidentEventRepository;
import com.devpulse.modules.incident.repository.IncidentRepository;
import com.devpulse.modules.monitoring.entity.MonitoringCheck;
import com.devpulse.modules.monitoring.repository.MonitoringCheckRepository;
import com.devpulse.modules.organization.entity.Membership;
import com.devpulse.modules.organization.entity.Organization;
import com.devpulse.modules.organization.repository.MembershipRepository;
import com.devpulse.modules.organization.repository.OrganizationRepository;
import com.devpulse.modules.service.entity.MonitoredService;
import com.devpulse.modules.service.repository.MonitoredServiceRepository;
import com.devpulse.modules.user.entity.User;
import com.devpulse.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final MonitoredServiceRepository serviceRepository;
    private final MonitoringCheckRepository checkRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           OrganizationRepository organizationRepository,
                           MembershipRepository membershipRepository,
                           MonitoredServiceRepository serviceRepository,
                           MonitoringCheckRepository checkRepository,
                           IncidentRepository incidentRepository,
                           IncidentEventRepository incidentEventRepository,
                           AlertRuleRepository alertRuleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.serviceRepository = serviceRepository;
        this.checkRepository = checkRepository;
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.alertRuleRepository = alertRuleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("owner@acme.corp").isPresent()) {
            log.info("Demo data already seeded.");
            return;
        }

        log.info("Seeding DevPulse demo organization, services, and users...");

        // 1. Users
        User admin = new User("Platform", "Admin", "admin@devpulse.io", passwordEncoder.encode("password123"), "PLATFORM_ADMIN");
        User owner = new User("Alex", "Vance", "owner@acme.corp", passwordEncoder.encode("password123"), "USER");
        User dev = new User("Sarah", "Connor", "dev@acme.corp", passwordEncoder.encode("password123"), "USER");
        User incMgr = new User("Gordon", "Freeman", "incident.mgr@acme.corp", passwordEncoder.encode("password123"), "USER");
        User viewer = new User("Ellie", "Williams", "viewer@acme.corp", passwordEncoder.encode("password123"), "USER");

        userRepository.saveAll(List.of(admin, owner, dev, incMgr, viewer));

        // 2. Organization: Acme Corp
        Organization acme = new Organization("Acme Corporation", "acme-corp", "ENTERPRISE");
        organizationRepository.save(acme);

        // 3. Memberships
        membershipRepository.save(new Membership(owner, acme, "ORG_OWNER"));
        membershipRepository.save(new Membership(dev, acme, "DEVELOPER"));
        membershipRepository.save(new Membership(incMgr, acme, "INCIDENT_MANAGER"));
        membershipRepository.save(new Membership(viewer, acme, "VIEWER"));

        // 4. Services (3 Backend Apps, 5 External APIs, 2 DB/Infra)
        MonitoredService s1 = new MonitoredService(acme.getId(), "Auth & Identity Service", "http://localhost:8082/api/v1/health", "GET", 200, 15, 3000, "PRODUCTION");
        MonitoredService s2 = new MonitoredService(acme.getId(), "Billing & Invoicing Engine", "https://httpbin.org/status/200", "GET", 200, 30, 5000, "PRODUCTION");
        MonitoredService s3 = new MonitoredService(acme.getId(), "Order Processing API", "https://httpbin.org/status/200", "GET", 200, 30, 4000, "STAGING");

        MonitoredService s4 = new MonitoredService(acme.getId(), "Stripe Payments Gateway", "https://api.stripe.com/healthcheck", "GET", 200, 60, 5000, "PRODUCTION");
        MonitoredService s5 = new MonitoredService(acme.getId(), "Twilio SMS Notification Gateway", "https://api.twilio.com", "GET", 200, 60, 5000, "PRODUCTION");
        MonitoredService s6 = new MonitoredService(acme.getId(), "SendGrid Mail Delivery Service", "https://sendgrid.com", "GET", 200, 60, 5000, "PRODUCTION");
        MonitoredService s7 = new MonitoredService(acme.getId(), "GitHub Webhooks API", "https://api.github.com", "GET", 200, 60, 5000, "PRODUCTION");
        MonitoredService s8 = new MonitoredService(acme.getId(), "AWS S3 Asset Storage CDN", "https://aws.amazon.com", "GET", 200, 60, 5000, "PRODUCTION");

        MonitoredService s9 = new MonitoredService(acme.getId(), "Primary PostgreSQL Replica", "http://localhost:8082/actuator/health", "GET", 200, 15, 2000, "PRODUCTION");
        MonitoredService s10 = new MonitoredService(acme.getId(), "Redis Cache Sentinel Cluster", "http://localhost:8082/api/v1/health", "GET", 200, 15, 2000, "PRODUCTION");

        s1.setCurrentStatus("UP");
        s1.setLastResponseTimeMs(42L);
        s2.setCurrentStatus("UP");
        s2.setLastResponseTimeMs(118L);
        s3.setCurrentStatus("UP");
        s3.setLastResponseTimeMs(85L);
        s4.setCurrentStatus("UP");
        s4.setLastResponseTimeMs(165L);
        s5.setCurrentStatus("UP");
        s5.setLastResponseTimeMs(142L);
        s6.setCurrentStatus("UP");
        s6.setLastResponseTimeMs(195L);
        s7.setCurrentStatus("UP");
        s7.setLastResponseTimeMs(88L);
        s8.setCurrentStatus("UP");
        s8.setLastResponseTimeMs(64L);
        s9.setCurrentStatus("UP");
        s9.setLastResponseTimeMs(15L);
        s10.setCurrentStatus("UP");
        s10.setLastResponseTimeMs(8L);

        List<MonitoredService> services = List.of(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10);
        serviceRepository.saveAll(services);

        // 5. Seed historical check records for telemetry charts
        Random random = new Random(42);
        LocalDateTime now = LocalDateTime.now();
        for (MonitoredService service : services) {
            for (int i = 24; i >= 0; i--) {
                LocalDateTime checkTime = now.minusHours(i).minusMinutes(random.nextInt(30));
                long latency = 20 + random.nextInt(120);
                boolean success = random.nextDouble() > 0.03; // 97% success rate
                MonitoringCheck check = new MonitoringCheck(
                        service.getId(),
                        acme.getId(),
                        success ? 200 : 503,
                        latency,
                        success,
                        success ? null : "Service Unavailable (503)"
                );
                checkRepository.save(check);
            }
        }

        // 6. Seed Alert Rules
        alertRuleRepository.save(new AlertRule(acme.getId(), null, "EMAIL", "sre-team@acme.corp", 15));
        alertRuleRepository.save(new AlertRule(acme.getId(), s1.getId(), "WEBHOOK", "https://webhook.site/devpulse-demo", 10));

        // 7. Seed one sample resolved incident and history
        Incident sampleIncident = new Incident(
                acme.getId(),
                s2.getId(),
                "Temporary Billing Gateway Latency Spike",
                "HTTP timeout detected on upstream payment gateway endpoint.",
                "MEDIUM"
        );
        sampleIncident.setStatus("RESOLVED");
        sampleIncident.setStartedAt(now.minusHours(5));
        sampleIncident.setAcknowledgedAt(now.minusHours(4));
        sampleIncident.setResolvedAt(now.minusHours(3));
        sampleIncident.setAssignedTo(incMgr);
        incidentRepository.save(sampleIncident);

        incidentEventRepository.save(new IncidentEvent(sampleIncident, null, "TRIGGERED", "Service reached 3 consecutive failures."));
        incidentEventRepository.save(new IncidentEvent(sampleIncident, incMgr, "ACKNOWLEDGED", "Investigating upstream network route."));
        incidentEventRepository.save(new IncidentEvent(sampleIncident, incMgr, "RESOLVED", "Traffic re-routed. Latency normalized."));

        log.info("Demo seed completed successfully!");
    }
}

