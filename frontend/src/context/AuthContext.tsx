import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, Organization, OrgMembership, UserRole } from '../types';
import { ApiClient } from '../services/api';
import { realtimeService } from '../services/websocket';

interface AuthContextType {
  user: User | null;
  activeOrg: Organization | null;
  role: UserRole | null;
  userOrgs: OrgMembership[];
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, pass: string, orgId?: string) => Promise<void>;
  register: (data: { firstName: string; lastName: string; email: string; password: string; organizationName?: string }) => Promise<void>;
  switchOrg: (orgId: string) => Promise<void>;
  logout: () => void;
  canManageServices: () => boolean;
  canManageIncidents: () => boolean;
  canManageAlerts: () => boolean;
  canManageTeam: () => boolean;
  isPlatformAdmin: () => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [activeOrg, setActiveOrg] = useState<Organization | null>(null);
  const [role, setRole] = useState<UserRole | null>(null);
  const [userOrgs, setUserOrgs] = useState<OrgMembership[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Attempt automatic session restore from localStorage
    const savedToken = localStorage.getItem('devpulse_token');
    const savedUser = localStorage.getItem('devpulse_user');
    const savedOrg = localStorage.getItem('devpulse_active_org');
    const savedRole = localStorage.getItem('devpulse_role') as UserRole;
    const savedOrgs = localStorage.getItem('devpulse_user_orgs');

    if (savedToken && savedUser && savedOrg) {
      try {
        const parsedUser = JSON.parse(savedUser);
        const parsedOrg = JSON.parse(savedOrg);
        const parsedOrgs = savedOrgs ? JSON.parse(savedOrgs) : [];
        setUser(parsedUser);
        setActiveOrg(parsedOrg);
        setRole(savedRole);
        setUserOrgs(parsedOrgs);

        // Connect WebSocket for this org
        realtimeService.connect(parsedOrg.id);
      } catch (e) {
        localStorage.clear();
      }
    }
    setIsLoading(false);
  }, []);

  const handleAuthSuccess = (res: any) => {
    localStorage.setItem('devpulse_token', res.accessToken);
    localStorage.setItem('devpulse_refresh_token', res.refreshToken);
    localStorage.setItem('devpulse_user', JSON.stringify(res.user));
    localStorage.setItem('devpulse_active_org', JSON.stringify(res.activeOrganization));
    localStorage.setItem('devpulse_active_org_id', res.activeOrganization?.id || '');
    localStorage.setItem('devpulse_role', res.organizationRole || 'USER');
    localStorage.setItem('devpulse_user_orgs', JSON.stringify(res.userOrganizations || []));

    setUser(res.user);
    setActiveOrg(res.activeOrganization);
    setRole(res.organizationRole as UserRole);
    setUserOrgs(res.userOrganizations || []);

    if (res.activeOrganization?.id) {
      realtimeService.connect(res.activeOrganization.id);
    }
  };

  const login = async (email: string, pass: string, orgId?: string) => {
    setIsLoading(true);
    try {
      const res = await ApiClient.login(email, pass, orgId);
      handleAuthSuccess(res);
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (data: any) => {
    setIsLoading(true);
    try {
      const res = await ApiClient.register(data);
      handleAuthSuccess(res);
    } finally {
      setIsLoading(false);
    }
  };

  const switchOrg = async (orgId: string) => {
    setIsLoading(true);
    try {
      const res = await ApiClient.switchOrg(orgId);
      handleAuthSuccess(res);
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    realtimeService.disconnect();
    localStorage.removeItem('devpulse_token');
    localStorage.removeItem('devpulse_refresh_token');
    localStorage.removeItem('devpulse_user');
    localStorage.removeItem('devpulse_active_org');
    localStorage.removeItem('devpulse_active_org_id');
    localStorage.removeItem('devpulse_role');
    localStorage.removeItem('devpulse_user_orgs');
    setUser(null);
    setActiveOrg(null);
    setRole(null);
    setUserOrgs([]);
  };

  const isPlatformAdmin = () => user?.globalRole === 'PLATFORM_ADMIN';
  const canManageServices = () => isPlatformAdmin() || role === 'ORG_OWNER' || role === 'DEVELOPER';
  const canManageIncidents = () => isPlatformAdmin() || role === 'ORG_OWNER' || role === 'INCIDENT_MANAGER' || role === 'DEVELOPER';
  const canManageAlerts = () => isPlatformAdmin() || role === 'ORG_OWNER' || role === 'DEVELOPER' || role === 'INCIDENT_MANAGER';
  const canManageTeam = () => isPlatformAdmin() || role === 'ORG_OWNER';

  return (
    <AuthContext.Provider
      value={{
        user,
        activeOrg,
        role,
        userOrgs,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        switchOrg,
        logout,
        canManageServices,
        canManageIncidents,
        canManageAlerts,
        canManageTeam,
        isPlatformAdmin,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
};

