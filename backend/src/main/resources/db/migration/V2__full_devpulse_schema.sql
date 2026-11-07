-- Migration V2: Complete DevPulse Schema

-- 1. Upgrade users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS global_role VARCHAR(50) NOT NULL DEFAULT 'USER';
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 2. Organizations
CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(150) NOT NULL UNIQUE,
    plan VARCHAR(50) NOT NULL DEFAULT 'FREE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Memberships (User to Org with RBAC)
CREATE TABLE IF NOT EXISTS memberships (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL, -- ORG_OWNER, DEVELOPER, INCIDENT_MANAGER, VIEWER
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_memberships_user_org UNIQUE (user_id, organization_id)
);

CREATE INDEX IF NOT EXISTS idx_memberships_user ON memberships(user_id);
CREATE INDEX IF NOT EXISTS idx_memberships_org ON memberships(organization_id);

-- 4. Invitations
CREATE TABLE IF NOT EXISTS invitations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    email VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invitations_token ON invitations(token);
CREATE INDEX IF NOT EXISTS idx_invitations_org ON invitations(organization_id);

-- 5. Monitored Services
CREATE TABLE IF NOT EXISTS monitored_services (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    http_method VARCHAR(10) NOT NULL DEFAULT 'GET',
    expected_status_code INT NOT NULL DEFAULT 200,
    check_interval_seconds INT NOT NULL DEFAULT 30,
    timeout_ms INT NOT NULL DEFAULT 5000,
    environment VARCHAR(50) NOT NULL DEFAULT 'PRODUCTION',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    current_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    consecutive_failures INT NOT NULL DEFAULT 0,
    consecutive_successes INT NOT NULL DEFAULT 0,
    failure_threshold INT NOT NULL DEFAULT 3,
    recovery_threshold INT NOT NULL DEFAULT 2,
    last_checked_at TIMESTAMP,
    last_response_time_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_services_org ON monitored_services(organization_id);
CREATE INDEX IF NOT EXISTS idx_services_active ON monitored_services(active);

-- 6. Monitoring Checks
CREATE TABLE IF NOT EXISTS monitoring_checks (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL REFERENCES monitored_services(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    http_status INT,
    response_time_ms BIGINT NOT NULL,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    checked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_checks_service_checked_at ON monitoring_checks(service_id, checked_at DESC);
CREATE INDEX IF NOT EXISTS idx_checks_service_success ON monitoring_checks(service_id, success);
CREATE INDEX IF NOT EXISTS idx_checks_org_checked_at ON monitoring_checks(organization_id, checked_at DESC);

-- 7. Incidents
CREATE TABLE IF NOT EXISTS incidents (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES monitored_services(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN', -- OPEN, ACKNOWLEDGED, RESOLVED, REOPENED
    severity VARCHAR(50) NOT NULL DEFAULT 'HIGH', -- CRITICAL, HIGH, MEDIUM, LOW
    assigned_to_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMP,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_incidents_org_status ON incidents(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_incidents_service_status ON incidents(service_id, status);

-- 8. Incident Events (Lifecycle Audit & Comments)
CREATE TABLE IF NOT EXISTS incident_events (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL, -- TRIGGERED, ACKNOWLEDGED, RESOLVED, REOPENED, COMMENT, ASSIGNED
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_incident_events_incident ON incident_events(incident_id, created_at ASC);

-- 9. Alert Rules
CREATE TABLE IF NOT EXISTS alert_rules (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    service_id UUID REFERENCES monitored_services(id) ON DELETE CASCADE,
    channel VARCHAR(50) NOT NULL DEFAULT 'EMAIL', -- EMAIL, WEBHOOK
    target VARCHAR(255) NOT NULL,
    cooldown_minutes INT NOT NULL DEFAULT 15,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_triggered_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_alert_rules_org ON alert_rules(organization_id);

-- 10. Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    incident_id UUID REFERENCES incidents(id) ON DELETE CASCADE,
    alert_rule_id UUID REFERENCES alert_rules(id) ON DELETE SET NULL,
    recipient VARCHAR(255) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SENT',
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_org ON notifications(organization_id, sent_at DESC);

-- 11. Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    user_email VARCHAR(150),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    details TEXT,
    ip_address VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_org ON audit_logs(organization_id, created_at DESC);

-- 12. Refresh Tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);

