const API_BASE = '/api/v1';

export class ApiClient {
  private static getToken(): string | null {
    return localStorage.getItem('devpulse_token');
  }

  private static getActiveOrgId(): string | null {
    return localStorage.getItem('devpulse_active_org_id');
  }

  public static async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const headers = new Headers(options.headers || {});
    
    if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
      headers.set('Content-Type', 'application/json');
    }

    const token = this.getToken();
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }

    const orgId = this.getActiveOrgId();
    if (orgId) {
      headers.set('X-Organization-Id', orgId);
    }

    const url = endpoint.startsWith('http') ? endpoint : `${API_BASE}${endpoint}`;
    const response = await fetch(url, {
      ...options,
      headers,
    });

    if (response.status === 401) {
      // Unauthorized: clear token if invalid
      if (!endpoint.includes('/auth/login') && !endpoint.includes('/auth/register')) {
        console.warn('Session expired or unauthorized');
      }
    }

    if (!response.ok) {
      let errorMessage = `HTTP error ${response.status}`;
      try {
        const text = await response.text();
        if (text) {
          try {
            const errorJson = JSON.parse(text);
            errorMessage = errorJson.message || errorJson.error || text;
          } catch {
            errorMessage = text;
          }
        }
      } catch {
        // fallback to status code
      }
      throw new Error(errorMessage);
    }

    if (response.status === 204) {
      return {} as T;
    }

    return response.json();
  }

  // Auth endpoints
  static login(email: string, password: string, organizationId?: string) {
    return this.request<any>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password, organizationId }),
    });
  }

  static register(data: { firstName: string; lastName: string; email: string; password: string; organizationName?: string }) {
    return this.request<any>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  static switchOrg(orgId: string) {
    return this.request<any>(`/auth/switch-org/${orgId}`, { method: 'POST' });
  }

  // Services
  static getServices(environment?: string) {
    const query = environment && environment !== 'ALL' ? `?environment=${environment}` : '';
    return this.request<any[]>(`/services${query}`);
  }

  static getService(id: string) {
    return this.request<any>(`/services/${id}`);
  }

  static createService(serviceData: any) {
    return this.request<any>('/services', {
      method: 'POST',
      body: JSON.stringify(serviceData),
    });
  }

  static updateService(id: string, serviceData: any) {
    return this.request<any>(`/services/${id}`, {
      method: 'PUT',
      body: JSON.stringify(serviceData),
    });
  }

  static toggleService(id: string) {
    return this.request<any>(`/services/${id}/toggle`, { method: 'PATCH' });
  }

  static deleteService(id: string) {
    return this.request<void>(`/services/${id}`, { method: 'DELETE' });
  }

  static triggerCheck(id: string) {
    return this.request<void>(`/services/${id}/check`, { method: 'POST' });
  }

  // Incidents
  static getIncidents(status?: string) {
    const query = status && status !== 'ALL' ? `?status=${status}` : '';
    return this.request<any[]>(`/incidents${query}`);
  }

  static getIncident(id: string) {
    return this.request<any>(`/incidents/${id}`);
  }

  static getIncidentEvents(id: string) {
    return this.request<any[]>(`/incidents/${id}/events`);
  }

  static acknowledgeIncident(id: string, comment?: string) {
    return this.request<any>(`/incidents/${id}/acknowledge`, {
      method: 'POST',
      body: JSON.stringify({ comment }),
    });
  }

  static resolveIncident(id: string, resolutionNotes?: string) {
    return this.request<any>(`/incidents/${id}/resolve`, {
      method: 'POST',
      body: JSON.stringify({ resolutionNotes }),
    });
  }

  static reopenIncident(id: string, reason?: string) {
    return this.request<any>(`/incidents/${id}/reopen`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
  }

  static assignIncident(id: string, userId: string) {
    return this.request<any>(`/incidents/${id}/assign`, {
      method: 'POST',
      body: JSON.stringify({ userId }),
    });
  }

  static addIncidentComment(id: string, comment: string) {
    return this.request<any>(`/incidents/${id}/comments`, {
      method: 'POST',
      body: JSON.stringify({ comment }),
    });
  }

  // Alerts
  static getAlertRules() {
    return this.request<any[]>('/alerts/rules');
  }

  static createAlertRule(ruleData: any) {
    return this.request<any>('/alerts/rules', {
      method: 'POST',
      body: JSON.stringify(ruleData),
    });
  }

  static toggleAlertRule(id: string) {
    return this.request<void>(`/alerts/rules/${id}/toggle`, { method: 'PATCH' });
  }

  static deleteAlertRule(id: string) {
    return this.request<void>(`/alerts/rules/${id}`, { method: 'DELETE' });
  }

  static getNotifications(limit = 50) {
    return this.request<any[]>(`/alerts/notifications?limit=${limit}`);
  }

  // Analytics
  static getOrgOverview() {
    return this.request<any>('/analytics/overview');
  }

  static getServiceAnalytics(serviceId: string) {
    return this.request<any>(`/analytics/services/${serviceId}`);
  }

  // Organization & Members
  static getMyOrgs() {
    return this.request<any[]>('/organizations/my');
  }

  static getOrgMembers(orgId: string) {
    return this.request<any[]>(`/organizations/${orgId}/members`);
  }

  static updateMemberRole(orgId: string, userId: string, role: string) {
    return this.request<void>(`/organizations/${orgId}/members/${userId}/role`, {
      method: 'PATCH',
      body: JSON.stringify({ role }),
    });
  }

  static removeMember(orgId: string, userId: string) {
    return this.request<void>(`/organizations/${orgId}/members/${userId}`, {
      method: 'DELETE',
    });
  }

  static getInvitations(orgId: string) {
    return this.request<any[]>(`/organizations/${orgId}/invitations`);
  }

  static inviteMember(orgId: string, email: string, role: string) {
    return this.request<any>(`/organizations/${orgId}/invitations`, {
      method: 'POST',
      body: JSON.stringify({ email, role }),
    });
  }

  static revokeInvitation(orgId: string, invitationId: string) {
    return this.request<void>(`/organizations/${orgId}/invitations/${invitationId}`, {
      method: 'DELETE',
    });
  }

  // Admin
  static getAdminStats() {
    return this.request<any>('/admin/stats');
  }

  static getGlobalAuditLogs(limit = 100) {
    return this.request<any[]>(`/admin/audit-logs?limit=${limit}`);
  }
}

