import React, { useEffect, useState } from 'react';
import { ApiClient } from '../services/api';
import { AlertRule, NotificationLog, MonitoredService } from '../types';
import { useAuth } from '../context/AuthContext';
import {
  BellRing,
  Plus,
  Mail,
  Webhook,
  Trash2,
  CheckCircle,
  Clock,
  X,
  History,
  ShieldAlert,
} from 'lucide-react';

export const AlertsView: React.FC = () => {
  const { canManageAlerts } = useAuth();
  const [rules, setRules] = useState<AlertRule[]>([]);
  const [notifications, setNotifications] = useState<NotificationLog[]>([]);
  const [services, setServices] = useState<MonitoredService[]>([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Form state
  const [channel, setChannel] = useState<'EMAIL' | 'WEBHOOK'>('EMAIL');
  const [target, setTarget] = useState('');
  const [serviceId, setServiceId] = useState<string>('');
  const [cooldownMinutes, setCooldownMinutes] = useState(15);
  const [formError, setFormError] = useState('');

  const fetchData = async () => {
    try {
      const [r, n, s] = await Promise.all([
        ApiClient.getAlertRules(),
        ApiClient.getNotifications(50),
        ApiClient.getServices(),
      ]);
      setRules(r);
      setNotifications(n);
      setServices(s);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCreateRule = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError('');
    try {
      await ApiClient.createAlertRule({
        channel,
        target,
        serviceId: serviceId || null,
        cooldownMinutes,
      });
      setIsModalOpen(false);
      setTarget('');
      fetchData();
    } catch (err: any) {
      setFormError(err.message || 'Failed to create alert rule');
    }
  };

  const handleToggle = async (id: string) => {
    try {
      await ApiClient.toggleAlertRule(id);
      fetchData();
    } catch (err) {
      console.error(err);
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Delete this alert rule?')) return;
    try {
      await ApiClient.deleteAlertRule(id);
      fetchData();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8 animate-in fade-in duration-300">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Alert Rules & Notifications</h1>
          <p className="text-sm text-slate-400 mt-1">
            Configure multi-channel notifications (Email, Webhook) with customizable cooldown periods to prevent inbox spam.
          </p>
        </div>

        {canManageAlerts() && (
          <button
            onClick={() => setIsModalOpen(true)}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-sm font-semibold shadow-lg shadow-indigo-600/25 transition shrink-0"
          >
            <Plus className="w-4 h-4" />
            <span>New Alert Rule</span>
          </button>
        )}
      </div>

      {/* Rules Table */}
      <div className="space-y-3">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider flex items-center gap-2">
          <BellRing className="w-4 h-4 text-indigo-400" />
          <span>Configured Notification Channels</span>
        </h3>

        {loading ? (
          <div className="py-10 text-center text-slate-400">Loading rules...</div>
        ) : rules.length === 0 ? (
          <div className="p-8 text-center rounded-2xl bg-slate-900/40 border border-slate-800 text-xs text-slate-400">
            No alert rules configured yet. Create one to receive incident notifications.
          </div>
        ) : (
          <div className="overflow-hidden rounded-2xl border border-slate-800 bg-slate-900/60 shadow-xl">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-950/60 border-b border-slate-800 text-xs text-slate-400 uppercase tracking-wider">
                <tr>
                  <th className="px-6 py-4 font-semibold">Channel</th>
                  <th className="px-4 py-4 font-semibold">Recipient / Target</th>
                  <th className="px-4 py-4 font-semibold">Scope</th>
                  <th className="px-4 py-4 font-semibold">Cooldown Period</th>
                  <th className="px-4 py-4 font-semibold">Status</th>
                  <th className="px-6 py-4 font-semibold text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80">
                {rules.map((rule) => {
                  const targetSvc = services.find((s) => s.id === rule.serviceId);
                  return (
                    <tr key={rule.id} className="hover:bg-slate-800/30 transition">
                      <td className="px-6 py-4 font-semibold text-white">
                        <div className="flex items-center gap-2">
                          {rule.channel === 'EMAIL' ? (
                            <Mail className="w-4 h-4 text-indigo-400" />
                          ) : (
                            <Webhook className="w-4 h-4 text-emerald-400" />
                          )}
                          <span>{rule.channel}</span>
                        </div>
                      </td>

                      <td className="px-4 py-4 font-mono text-xs text-slate-200">{rule.target}</td>

                      <td className="px-4 py-4 text-xs text-slate-300">
                        {targetSvc ? (
                          <span className="font-semibold text-indigo-300">{targetSvc.name}</span>
                        ) : (
                          <span className="text-slate-400">All Organization Services</span>
                        )}
                      </td>

                      <td className="px-4 py-4 text-xs text-slate-300">
                        <div className="flex items-center gap-1">
                          <Clock className="w-3.5 h-3.5 text-slate-500" />
                          <span>{rule.cooldownMinutes} minutes</span>
                        </div>
                      </td>

                      <td className="px-4 py-4">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            rule.active
                              ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                              : 'bg-slate-800 text-slate-400'
                          }`}
                        >
                          {rule.active ? 'ACTIVE' : 'MUTED'}
                        </span>
                      </td>

                      <td className="px-6 py-4 text-right">
                        {canManageAlerts() && (
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={() => handleToggle(rule.id)}
                              className="px-2.5 py-1 text-xs rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300"
                            >
                              {rule.active ? 'Mute' : 'Enable'}
                            </button>
                            <button
                              onClick={() => handleDelete(rule.id)}
                              className="p-1.5 rounded-lg bg-slate-800 hover:bg-rose-950/60 hover:text-rose-400 text-slate-300"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Notifications Audit Log */}
      <div className="space-y-3">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider flex items-center gap-2">
          <History className="w-4 h-4 text-indigo-400" />
          <span>Notification Delivery Log</span>
        </h3>

        {notifications.length === 0 ? (
          <div className="p-8 text-center rounded-2xl bg-slate-900/40 border border-slate-800 text-xs text-slate-400">
            No alert notifications triggered yet.
          </div>
        ) : (
          <div className="overflow-hidden rounded-2xl border border-slate-800 bg-slate-900/60 shadow-xl">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-950/60 border-b border-slate-800 text-xs text-slate-400 uppercase tracking-wider">
                <tr>
                  <th className="px-6 py-3 font-semibold">Title</th>
                  <th className="px-4 py-3 font-semibold">Recipient</th>
                  <th className="px-4 py-3 font-semibold">Channel</th>
                  <th className="px-4 py-3 font-semibold">Time</th>
                  <th className="px-6 py-3 font-semibold text-right">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80 text-xs">
                {notifications.map((n) => (
                  <tr key={n.id} className="hover:bg-slate-800/20">
                    <td className="px-6 py-3 font-medium text-slate-200 max-w-sm truncate">{n.title}</td>
                    <td className="px-4 py-3 font-mono text-slate-400">{n.recipient}</td>
                    <td className="px-4 py-3 text-slate-300 font-semibold">{n.channel}</td>
                    <td className="px-4 py-3 text-slate-400">{new Date(n.sentAt).toLocaleString()}</td>
                    <td className="px-6 py-3 text-right">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                        {n.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal: New Alert Rule */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in">
          <div className="relative w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800">
              <h3 className="font-bold text-white">Create Alert Notification Rule</h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateRule} className="p-6 space-y-4">
              {formError && (
                <div className="p-3 text-xs rounded-xl bg-rose-500/15 border border-rose-500/30 text-rose-300">
                  {formError}
                </div>
              )}

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Notification Channel</label>
                <select
                  value={channel}
                  onChange={(e) => setChannel(e.target.value as any)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500"
                >
                  <option value="EMAIL">Email Dispatch</option>
                  <option value="WEBHOOK">HTTP Webhook URL</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">
                  {channel === 'EMAIL' ? 'Destination Email Address' : 'Webhook Endpoint URL'}
                </label>
                <input
                  type={channel === 'EMAIL' ? 'email' : 'url'}
                  required
                  value={target}
                  onChange={(e) => setTarget(e.target.value)}
                  placeholder={channel === 'EMAIL' ? 'sre-alerts@company.com' : 'https://api.company.com/webhook'}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500 font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Applies To Service</label>
                <select
                  value={serviceId}
                  onChange={(e) => setServiceId(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500"
                >
                  <option value="">All Services in Organization (Wildcard)</option>
                  {services.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name} ({s.environment})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">
                  Alert Cooldown Window (minutes)
                </label>
                <input
                  type="number"
                  min="1"
                  max="1440"
                  required
                  value={cooldownMinutes}
                  onChange={(e) => setCooldownMinutes(parseInt(e.target.value))}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500"
                />
                <p className="text-[11px] text-slate-500 mt-1">
                  Consecutive alerts within this window are throttled to prevent spam.
                </p>
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold rounded-xl shadow-lg shadow-indigo-600/25"
                >
                  Create Rule
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

