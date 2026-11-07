export type UserRole = 'PLATFORM_ADMIN' | 'ORG_OWNER' | 'DEVELOPER' | 'INCIDENT_MANAGER' | 'VIEWER' | 'USER';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  globalRole: string;
}

export interface Organization {
  id: string;
  name: string;
  slug: string;
  plan: string;
  createdAt?: string;
}

export interface OrgMembership {
  organizationId: string;
  organizationName: string;
  slug: string;
  role: UserRole;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
  activeOrganization: Organization;
  organizationRole: UserRole;
  userOrganizations: OrgMembership[];
}

export interface MonitoredService {
  id: string;
  organizationId: string;
  name: string;
  url: string;
  httpMethod: string;
  expectedStatusCode: number;
  checkIntervalSeconds: number;
  timeoutMs: number;
  environment: 'PRODUCTION' | 'STAGING' | 'DEVELOPMENT';
  active: boolean;
  currentStatus: 'UP' | 'DOWN' | 'DEGRADED' | 'PAUSED' | 'PENDING';
  consecutiveFailures: number;
  consecutiveSuccesses: number;
  failureThreshold: number;
  recoveryThreshold: number;
  lastCheckedAt?: string;
  lastResponseTimeMs?: number;
  createdAt?: string;
}

export interface MonitoringCheck {
  id: string;
  serviceId: string;
  organizationId: string;
  httpStatus?: number;
  responseTimeMs: number;
  success: boolean;
  errorMessage?: string;
  checkedAt: string;
}

export interface Incident {
  id: string;
  organizationId: string;
  serviceId: string;
  title: string;
  description?: string;
  status: 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED' | 'REOPENED';
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  assignedTo?: User;
  startedAt: string;
  acknowledgedAt?: string;
  resolvedAt?: string;
  createdAt: string;
}

export interface IncidentEvent {
  id: string;
  incidentId: string;
  user?: User;
  eventType: 'TRIGGERED' | 'ACKNOWLEDGED' | 'RESOLVED' | 'REOPENED' | 'COMMENT' | 'ASSIGNED';
  comment: string;
  createdAt: string;
}

export interface AlertRule {
  id: string;
  organizationId: string;
  serviceId?: string;
  channel: 'EMAIL' | 'WEBHOOK';
  target: string;
  cooldownMinutes: number;
  active: boolean;
  lastTriggeredAt?: string;
}

export interface NotificationLog {
  id: string;
  organizationId: string;
  incidentId?: string;
  recipient: string;
  channel: string;
  title: string;
  message: string;
  status: string;
  sentAt: string;
}

export interface TeamMember {
  membershipId: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  joinedAt: string;
}

export interface Invitation {
  id: string;
  organizationId: string;
  email: string;
  role: string;
  token: string;
  status: string;
  expiresAt: string;
  createdAt: string;
}

export interface OrgOverview {
  totalServices: number;
  upServices: number;
  downServices: number;
  degradedServices: number;
  openIncidents: number;
  monthlyIncidents: number;
  uptime24h: number;
  avgLatencyMs: number;
  slaTarget: number;
  slaCompliant: boolean;
}

export interface ServiceAnalytics {
  serviceId: string;
  serviceName: string;
  environment: string;
  currentStatus: string;
  lastResponseTimeMs: number;
  uptime24h: number;
  uptime7d: number;
  uptime30d: number;
  avgLatency24h: number;
  incidents30d: number;
  errorRate24h: number;
  slaTarget: number;
  slaCompliant: boolean;
  recentChecks: Array<{
    timestamp: string;
    responseTimeMs: number;
    success: boolean;
    status: number;
  }>;
}

export interface SystemStats {
  totalUsers: number;
  totalOrganizations: number;
  totalServices: number;
  totalChecksToday: number;
}

export interface AuditLog {
  id: string;
  organizationId?: string;
  userId?: string;
  userEmail?: string;
  action: string;
  entityType: string;
  entityId?: string;
  details?: string;
  ipAddress?: string;
  createdAt: string;
}

