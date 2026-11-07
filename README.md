# DevPulse — SaaS Monitoring & Incident Management Platform

> **"Monitor your applications, detect failures early, and manage incidents from one unified platform."**

DevPulse is a full-featured SaaS observability and incident orchestration platform designed to help engineering organizations monitor applications, track API latency, detect outages in real time, and streamline incident response.

---

## 🚀 Key Features

1. **Authentication & Multi-Tenancy (RBAC)**:
   - Organization-level data isolation.
   - JWT authentication & refresh tokens.
   - Fine-grained RBAC: `ORG_OWNER`, `DEVELOPER`, `INCIDENT_MANAGER`, `VIEWER`, and `PLATFORM_ADMIN`.
   - Team invitations with secure tokens.

2. **Application & API Management**:
   - Register backend microservices, third-party APIs, and databases.
   - Configurable check cadences (5s to 1h), HTTP methods, status codes, and timeouts.
   - Environment tagging (`PRODUCTION`, `STAGING`, `DEVELOPMENT`).
   - Pause / resume monitoring in 1 click.

3. **High-Performance Monitoring Engine**:
   - Scheduled health checks via Spring Scheduler using a bounded thread pool (10 worker threads).
   - Accurate millisecond latency measurements and timeout detection.
   - Manual on-demand check triggers.

4. **Incident State Machine**:
   - **Failure Threshold**: Automatically triggers an `OPEN` incident after **3 consecutive failed checks**.
   - **Auto-Recovery**: Automatically transitions incidents to `RESOLVED` after **2 consecutive successful checks**.
   - **Duplicate Prevention**: Reuses active incidents instead of spawning duplicate alerts.
   - Incident assignment, resolution notes, and immutable audit events timeline.

5. **Alert Management**:
   - Multi-channel notification dispatch (**Email** & **Webhook**).
   - Configurable **cooldown periods** (e.g. 15 min) per alert rule to avoid inbox flooding.
   - Notification delivery history audit logs.

6. **Analytics & SLA Telemetry**:
   - Rolling availability percentages: 24-hour, 7-day, and 30-day uptime.
   - Interactive latency trend area charts with Recharts.
   - SLA commitment compliance tracking against a 99.9% target (`RESPECTED` / `BREACHED`).

7. **Real-Time Dashboard**:
   - WebSockets (**STOMP over SockJS**) streaming live health checks and status changes.
   - Real-time outage alerts without page refreshing.

8. **Platform Administration & Security**:
   - Platform-wide statistics (users, orgs, services, daily checks).
   - Security audit trail (`audit_logs`) recording administrative actions.

---

## 🛠️ Tech Stack

- **Backend**: Java 23, Spring Boot 3.x, Spring Security, Spring Data JPA, Spring WebSocket (STOMP), Flyway
- **Database**: PostgreSQL 17
- **Caching & Coordination**: Redis
- **Frontend**: React, TypeScript, Vite, Tailwind CSS, Lucide Icons, Recharts
- **Testing**: JUnit 5, Mockito, Spring Boot Test

---

## ⚡ Quick Start

### 1. Prerequisites
- Java 21+ (Java 23 recommended)
- Node.js 20+ & npm
- Docker & Docker Compose

### 2. Start PostgreSQL
```bash
docker compose up -d postgres
```

### 3. Start Backend (Port 8082)
```bash
cd backend
mvn spring-boot:run
```
Backend health check: [http://localhost:8082/api/v1/health](http://localhost:8082/api/v1/health)

### 4. Start Frontend (Port 5173)
```bash
cd frontend
npm install
npm run dev
```
Access the dashboard: [http://localhost:5173](http://localhost:5173)

---

## 👥 Demo Personas (Pre-seeded)

Use these pre-configured accounts (Password: `password123`) to test each RBAC role:

| Persona | Role | Email | Scope |
|---|---|---|---|
| **Alex Vance** | `ORG_OWNER` | `owner@acme.corp` | Full org management, billing & team roles |
| **Sarah Connor** | `DEVELOPER` | `dev@acme.corp` | Register & configure services & checks |
| **Gordon Freeman** | `INCIDENT_MANAGER` | `incident.mgr@acme.corp` | Acknowledge & resolve incidents |
| **Ellie Williams** | `VIEWER` | `viewer@acme.corp` | Read-only dashboards and metrics |
| **Admin** | `PLATFORM_ADMIN` | `admin@devpulse.io` | Global multi-tenant admin & platform audit |

---

## 📄 License
This project is licensed under the MIT License.
