import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Activity, Shield, Users, LogOut, Radio, ChevronDown, Sparkles } from 'lucide-react';

interface NavbarProps {
  onOpenAuth: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onOpenAuth }) => {
  const { user, activeOrg, role, userOrgs, switchOrg, logout, isAuthenticated } = useAuth();

  const getRoleBadgeColor = (r: string | null) => {
    switch (r) {
      case 'PLATFORM_ADMIN': return 'bg-purple-500/20 text-purple-300 border-purple-500/30';
      case 'ORG_OWNER': return 'bg-amber-500/20 text-amber-300 border-amber-500/30';
      case 'DEVELOPER': return 'bg-indigo-500/20 text-indigo-300 border-indigo-500/30';
      case 'INCIDENT_MANAGER': return 'bg-rose-500/20 text-rose-300 border-rose-500/30';
      case 'VIEWER': return 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30';
      default: return 'bg-slate-700 text-slate-300 border-slate-600';
    }
  };

  return (
    <header className="h-16 border-b border-slate-800 bg-slate-900/80 backdrop-blur sticky top-0 z-40 px-6 flex items-center justify-between">
      {/* Brand */}
      <div className="flex items-center gap-3">
        <div className="relative flex items-center justify-center w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-indigo-700 shadow-lg shadow-indigo-500/25">
          <Activity className="w-5 h-5 text-white animate-pulse" />
          <span className="absolute -top-1 -right-1 w-3 h-3 bg-emerald-500 rounded-full border-2 border-slate-950 animate-ping" />
          <span className="absolute -top-1 -right-1 w-3 h-3 bg-emerald-500 rounded-full border-2 border-slate-950" />
        </div>
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xl font-bold tracking-tight text-white">DevPulse</span>
            <span className="text-[10px] uppercase font-semibold tracking-wider px-1.5 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
              SaaS v1.0
            </span>
          </div>
          <p className="text-xs text-slate-400 hidden sm:block">Unified Monitoring & Incident Platform</p>
        </div>
      </div>

      {/* Right controls */}
      <div className="flex items-center gap-4">
        {isAuthenticated ? (
          <>
            {/* Live Indicator */}
            <div className="hidden md:flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-xs font-medium text-emerald-400">
              <Radio className="w-3.5 h-3.5 animate-pulse" />
              <span>LIVE ENGINE</span>
            </div>

            {/* Org Switcher */}
            {userOrgs.length > 0 && activeOrg && (
              <div className="relative group">
                <button className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-800/80 hover:bg-slate-800 border border-slate-700 text-sm text-slate-200 transition">
                  <Users className="w-4 h-4 text-indigo-400" />
                  <span className="font-medium max-w-[140px] truncate">{activeOrg.name}</span>
                  <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
                </button>
                {userOrgs.length > 1 && (
                  <div className="absolute right-0 mt-1 w-52 py-1 bg-slate-900 border border-slate-800 rounded-lg shadow-xl hidden group-hover:block z-50">
                    <div className="px-3 py-1 text-xs font-medium text-slate-400 uppercase tracking-wider border-b border-slate-800">
                      Switch Organization
                    </div>
                    {userOrgs.map((org) => (
                      <button
                        key={org.organizationId}
                        onClick={() => switchOrg(org.organizationId)}
                        className={`w-full text-left px-3 py-2 text-sm flex items-center justify-between hover:bg-slate-800 transition ${
                          org.organizationId === activeOrg.id ? 'text-indigo-400 font-semibold' : 'text-slate-300'
                        }`}
                      >
                        <span className="truncate">{org.organizationName}</span>
                        <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 uppercase">
                          {org.role}
                        </span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* Role Badge */}
            <div className={`px-2.5 py-1 rounded-full text-xs font-semibold border ${getRoleBadgeColor(role)}`}>
              {role || 'MEMBER'}
            </div>

            {/* User Profile & Demo Switcher */}
            <div className="flex items-center gap-3 pl-2 border-l border-slate-800">
              <button
                onClick={onOpenAuth}
                title="Switch demo persona"
                className="flex items-center gap-1.5 text-xs text-indigo-300 bg-indigo-950/60 hover:bg-indigo-900/60 border border-indigo-700/40 px-2.5 py-1.5 rounded-lg transition"
              >
                <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
                <span className="hidden sm:inline">Role Switcher</span>
              </button>

              <div className="text-right hidden sm:block">
                <div className="text-sm font-medium text-slate-200">
                  {user?.firstName} {user?.lastName}
                </div>
                <div className="text-xs text-slate-400">{user?.email}</div>
              </div>

              <button
                onClick={logout}
                title="Log out"
                className="p-2 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          </>
        ) : (
          <button
            onClick={onOpenAuth}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-lg shadow-lg shadow-indigo-600/25 transition"
          >
            <Shield className="w-4 h-4" />
            <span>Sign In / Demo Roles</span>
          </button>
        )}
      </div>
    </header>
  );
};

