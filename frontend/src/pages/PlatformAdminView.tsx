import React, { useEffect, useState } from 'react';
import { ApiClient } from '../services/api';
import { SystemStats, AuditLog } from '../types';
import {
  ShieldAlert,
  Users,
  Building,
  Server,
  Activity,
  ScrollText,
  RefreshCw,
} from 'lucide-react';

export const PlatformAdminView: React.FC = () => {
  const [stats, setStats] = useState<SystemStats | null>(null);
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    try {
      const [s, l] = await Promise.all([
        ApiClient.getAdminStats(),
        ApiClient.getGlobalAuditLogs(100),
      ]);
      setStats(s);
      setLogs(l);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8 animate-in fade-in duration-300">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-purple-400 font-semibold text-xs uppercase tracking-wider mb-1">
            <ShieldAlert className="w-4 h-4" />
            <span>Platform-Level Governance</span>
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Platform Administration & Audit Logs</h1>
          <p className="text-sm text-slate-400 mt-1">
            Global metrics across all tenants, security events, and immutable system audit logs.
          </p>
        </div>

        <button
          onClick={fetchData}
          className="flex items-center gap-2 px-3.5 py-2 bg-slate-800/80 hover:bg-slate-800 border border-slate-700 rounded-xl text-xs font-medium text-slate-300 transition shrink-0"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          <span>Refresh Audit Stream</span>
        </button>
      </div>

      {/* Global Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">Total Platform Users</span>
            <Users className="w-4 h-4 text-purple-400" />
          </div>
          <div className="mt-3 text-2xl font-bold text-white">
            {stats?.totalUsers ?? '—'}
          </div>
          <div className="mt-2 text-xs text-slate-400">All registered tenant accounts</div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">Organizations</span>
            <Building className="w-4 h-4 text-indigo-400" />
          </div>
          <div className="mt-3 text-2xl font-bold text-white">
            {stats?.totalOrganizations ?? '—'}
          </div>
          <div className="mt-2 text-xs text-slate-400">Active SaaS tenant spaces</div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">Monitored Services</span>
            <Server className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="mt-3 text-2xl font-bold text-white">
            {stats?.totalServices ?? '—'}
          </div>
          <div className="mt-2 text-xs text-slate-400">Applications, APIs & databases</div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">Checks Executed Today</span>
            <Activity className="w-4 h-4 text-amber-400" />
          </div>
          <div className="mt-3 text-2xl font-bold text-white">
            {stats?.totalChecksToday ?? '—'}
          </div>
          <div className="mt-2 text-xs text-slate-400">Via Spring scheduler pool</div>
        </div>
      </div>

      {/* Audit Logs Table */}
      <div className="space-y-3">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider flex items-center gap-2">
          <ScrollText className="w-4 h-4 text-purple-400" />
          <span>Security & Administration Audit Trail</span>
        </h3>

        {loading ? (
          <div className="py-12 text-center text-slate-400">Loading audit records...</div>
        ) : logs.length === 0 ? (
          <div className="p-8 text-center rounded-2xl bg-slate-900/40 border border-slate-800 text-xs text-slate-400">
            No audit records found.
          </div>
        ) : (
          <div className="overflow-hidden rounded-2xl border border-slate-800 bg-slate-900/60 shadow-xl">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-950/60 border-b border-slate-800 text-xs text-slate-400 uppercase tracking-wider">
                <tr>
                  <th className="px-6 py-3 font-semibold">Action</th>
                  <th className="px-4 py-3 font-semibold">User / Email</th>
                  <th className="px-4 py-3 font-semibold">Entity Type</th>
                  <th className="px-4 py-3 font-semibold">Details</th>
                  <th className="px-6 py-3 font-semibold text-right">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80 text-xs">
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-800/20">
                    <td className="px-6 py-3">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-800 text-purple-300 border border-purple-500/20">
                        {log.action}
                      </span>
                    </td>
                    <td className="px-4 py-3 font-medium text-slate-200">{log.userEmail || 'System'}</td>
                    <td className="px-4 py-3 font-mono text-slate-400">{log.entityType}</td>
                    <td className="px-4 py-3 text-slate-300 max-w-md truncate">{log.details || '—'}</td>
                    <td className="px-6 py-3 text-right text-slate-400">{new Date(log.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

