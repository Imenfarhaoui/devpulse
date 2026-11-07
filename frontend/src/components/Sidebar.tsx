import React from 'react';
import { useAuth } from '../context/AuthContext';
import {
  LayoutDashboard,
  Server,
  AlertTriangle,
  BarChart3,
  BellRing,
  Users2,
  ShieldAlert,
} from 'lucide-react';

export type TabId = 'overview' | 'services' | 'incidents' | 'analytics' | 'alerts' | 'team' | 'admin';

interface SidebarProps {
  activeTab: TabId;
  onSelectTab: (tab: TabId) => void;
  openIncidentsCount: number;
}

export const Sidebar: React.FC<SidebarProps> = ({ activeTab, onSelectTab, openIncidentsCount }) => {
  const { isPlatformAdmin } = useAuth();

  const navItems = [
    { id: 'overview' as TabId, label: 'Live Dashboard', icon: LayoutDashboard },
    { id: 'services' as TabId, label: 'Applications & APIs', icon: Server },
    {
      id: 'incidents' as TabId,
      label: 'Incidents Hub',
      icon: AlertTriangle,
      badge: openIncidentsCount > 0 ? openIncidentsCount : undefined,
      badgeColor: 'bg-rose-500 text-white',
    },
    { id: 'analytics' as TabId, label: 'Analytics & SLA', icon: BarChart3 },
    { id: 'alerts' as TabId, label: 'Alert Rules & Logs', icon: BellRing },
    { id: 'team' as TabId, label: 'Organization & RBAC', icon: Users2 },
  ];

  if (isPlatformAdmin()) {
    navItems.push({
      id: 'admin' as TabId,
      label: 'Platform Administration',
      icon: ShieldAlert,
      badge: undefined,
      badgeColor: '',
    });
  }

  return (
    <aside className="w-64 border-r border-slate-800 bg-slate-900/50 flex flex-col shrink-0">
      <div className="p-4 flex-1 space-y-1.5">
        <div className="px-3 py-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">
          Monitoring & Ops
        </div>
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onSelectTab(item.id)}
              className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium transition ${
                isActive
                  ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/25'
                  : 'text-slate-400 hover:text-slate-100 hover:bg-slate-800/60'
              }`}
            >
              <div className="flex items-center gap-3">
                <Icon className={`w-4 h-4 ${isActive ? 'text-white' : 'text-slate-400'}`} />
                <span>{item.label}</span>
              </div>
              {item.badge !== undefined && (
                <span className={`px-2 py-0.5 rounded-full text-xs font-bold ${item.badgeColor || 'bg-slate-800 text-slate-300'}`}>
                  {item.badge}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* Footer Info */}
      <div className="p-4 border-t border-slate-800/80">
        <div className="p-3 rounded-xl bg-slate-800/40 border border-slate-800 text-xs text-slate-400 space-y-1">
          <div className="flex items-center justify-between font-medium text-slate-300">
            <span>Engine Thread Pool</span>
            <span className="text-emerald-400">10 Workers</span>
          </div>
          <div className="flex items-center justify-between">
            <span>Fail Threshold</span>
            <span className="text-slate-300">3 checks</span>
          </div>
          <div className="flex items-center justify-between">
            <span>Recovery Threshold</span>
            <span className="text-slate-300">2 checks</span>
          </div>
        </div>
      </div>
    </aside>
  );
};

