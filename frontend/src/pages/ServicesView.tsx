import React, { useEffect, useState } from 'react';
import { ApiClient } from '../services/api';
import { MonitoredService } from '../types';
import { useAuth } from '../context/AuthContext';
import {
  Plus,
  Server,
  Play,
  Pause,
  Trash2,
  Edit2,
  RefreshCw,
  ExternalLink,
  X,
  Clock,
  Layers,
} from 'lucide-react';

interface ServicesViewProps {
  onSelectServiceForAnalytics?: (serviceId: string) => void;
}

export const ServicesView: React.FC<ServicesViewProps> = ({ onSelectServiceForAnalytics }) => {
  const { canManageServices } = useAuth();
  const [services, setServices] = useState<MonitoredService[]>([]);
  const [loading, setLoading] = useState(true);
  const [envFilter, setEnvFilter] = useState<string>('ALL');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingService, setEditingService] = useState<MonitoredService | null>(null);
  const [checkingId, setCheckingId] = useState<string | null>(null);

  // Form state
  const [name, setName] = useState('');
  const [url, setUrl] = useState('');
  const [httpMethod, setHttpMethod] = useState('GET');
  const [expectedStatusCode, setExpectedStatusCode] = useState(200);
  const [checkIntervalSeconds, setCheckIntervalSeconds] = useState(30);
  const [timeoutMs, setTimeoutMs] = useState(5000);
  const [environment, setEnvironment] = useState<'PRODUCTION' | 'STAGING' | 'DEVELOPMENT'>('PRODUCTION');
  const [formError, setFormError] = useState('');

  const fetchServices = async () => {
    try {
      const data = await ApiClient.getServices(envFilter);
      setServices(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchServices();
  }, [envFilter]);

  const openCreateModal = () => {
    setEditingService(null);
    setName('');
    setUrl('');
    setHttpMethod('GET');
    setExpectedStatusCode(200);
    setCheckIntervalSeconds(30);
    setTimeoutMs(5000);
    setEnvironment('PRODUCTION');
    setFormError('');
    setIsModalOpen(true);
  };

  const openEditModal = (svc: MonitoredService) => {
    setEditingService(svc);
    setName(svc.name);
    setUrl(svc.url);
    setHttpMethod(svc.httpMethod);
    setExpectedStatusCode(svc.expectedStatusCode);
    setCheckIntervalSeconds(svc.checkIntervalSeconds);
    setTimeoutMs(svc.timeoutMs);
    setEnvironment(svc.environment);
    setFormError('');
    setIsModalOpen(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError('');

    try {
      if (editingService) {
        await ApiClient.updateService(editingService.id, {
          name,
          url,
          httpMethod,
          expectedStatusCode,
          checkIntervalSeconds,
          timeoutMs,
          environment,
        });
      } else {
        await ApiClient.createService({
          name,
          url,
          httpMethod,
          expectedStatusCode,
          checkIntervalSeconds,
          timeoutMs,
          environment,
        });
      }
      setIsModalOpen(false);
      fetchServices();
    } catch (err: any) {
      setFormError(err.message || 'Failed to save service');
    }
  };

  const handleToggle = async (id: string) => {
    try {
      await ApiClient.toggleService(id);
      fetchServices();
    } catch (err) {
      console.error(err);
    }
  };

  const handleDelete = async (id: string, name: string) => {
    if (!window.confirm(`Delete service "${name}"? This will remove all associated checks and incidents.`)) {
      return;
    }
    try {
      await ApiClient.deleteService(id);
      fetchServices();
    } catch (err) {
      console.error(err);
    }
  };

  const handleManualCheck = async (id: string) => {
    setCheckingId(id);
    try {
      await ApiClient.triggerCheck(id);
      setTimeout(() => {
        setCheckingId(null);
        fetchServices();
      }, 1000);
    } catch (e) {
      setCheckingId(null);
    }
  };

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-6 animate-in fade-in duration-300">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Applications & API Endpoints</h1>
          <p className="text-sm text-slate-400 mt-1">
            Register backend microservices, third-party APIs, and infrastructure endpoints with custom intervals.
          </p>
        </div>

        {canManageServices() && (
          <button
            onClick={openCreateModal}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-sm font-semibold shadow-lg shadow-indigo-600/25 transition shrink-0"
          >
            <Plus className="w-4 h-4" />
            <span>Register New Service</span>
          </button>
        )}
      </div>

      {/* Environment Filters */}
      <div className="flex items-center gap-2 border-b border-slate-800 pb-4">
        {['ALL', 'PRODUCTION', 'STAGING', 'DEVELOPMENT'].map((env) => (
          <button
            key={env}
            onClick={() => setEnvFilter(env)}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition ${
              envFilter === env
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'bg-slate-900 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
            }`}
          >
            {env === 'ALL' ? 'All Environments' : env}
          </button>
        ))}
      </div>

      {/* Services Table */}
      {loading ? (
        <div className="py-20 text-center text-slate-400">Loading services...</div>
      ) : services.length === 0 ? (
        <div className="p-12 text-center rounded-2xl bg-slate-900/40 border border-slate-800 space-y-3">
          <Server className="w-10 h-10 text-slate-600 mx-auto" />
          <h3 className="text-base font-semibold text-slate-300">No Monitored Services Found</h3>
          <p className="text-xs text-slate-400 max-w-md mx-auto">
            {envFilter !== 'ALL'
              ? `No services registered under environment "${envFilter}".`
              : 'Add your backend applications and API endpoints to start health monitoring.'}
          </p>
          {canManageServices() && (
            <button
              onClick={openCreateModal}
              className="mt-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-semibold transition"
            >
              Register First Service
            </button>
          )}
        </div>
      ) : (
        <div className="overflow-hidden rounded-2xl border border-slate-800 bg-slate-900/60 shadow-xl">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-950/60 border-b border-slate-800 text-xs text-slate-400 uppercase tracking-wider">
              <tr>
                <th className="px-6 py-4 font-semibold">Service / URL</th>
                <th className="px-4 py-4 font-semibold">Status</th>
                <th className="px-4 py-4 font-semibold">Response</th>
                <th className="px-4 py-4 font-semibold">Config & Cadence</th>
                <th className="px-4 py-4 font-semibold">State Machine</th>
                <th className="px-6 py-4 font-semibold text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {services.map((svc) => (
                <tr key={svc.id} className="hover:bg-slate-800/30 transition">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-white">{svc.name}</span>
                      <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 font-bold uppercase">
                        {svc.environment}
                      </span>
                    </div>
                    <div className="text-xs text-slate-400 font-mono mt-0.5 truncate max-w-sm" title={svc.url}>
                      <span className="font-bold text-slate-400">{svc.httpMethod}</span> {svc.url}
                    </div>
                  </td>

                  <td className="px-4 py-4">
                    <span
                      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold ${
                        svc.currentStatus === 'UP'
                          ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                          : svc.currentStatus === 'DOWN'
                          ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30 animate-pulse'
                          : svc.currentStatus === 'DEGRADED'
                          ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                          : 'bg-slate-800 text-slate-400 border border-slate-700'
                      }`}
                    >
                      ● {svc.currentStatus}
                    </span>
                  </td>

                  <td className="px-4 py-4">
                    <div className="font-mono text-slate-200 font-semibold">
                      {svc.lastResponseTimeMs != null ? `${svc.lastResponseTimeMs}ms` : '—'}
                    </div>
                    <div className="text-[11px] text-slate-400">
                      {svc.lastCheckedAt ? new Date(svc.lastCheckedAt).toLocaleTimeString() : 'Never'}
                    </div>
                  </td>

                  <td className="px-4 py-4 text-xs text-slate-300">
                    <div>every {svc.checkIntervalSeconds}s</div>
                    <div className="text-[11px] text-slate-400">timeout: {svc.timeoutMs}ms • expected: {svc.expectedStatusCode}</div>
                  </td>

                  <td className="px-4 py-4 text-xs">
                    <div className="flex items-center gap-3">
                      <div>
                        <span className="text-slate-400">Fails: </span>
                        <span className={`font-mono font-bold ${svc.consecutiveFailures > 0 ? 'text-rose-400' : 'text-slate-300'}`}>
                          {svc.consecutiveFailures}/{svc.failureThreshold}
                        </span>
                      </div>
                      <div>
                        <span className="text-slate-400">Pass: </span>
                        <span className={`font-mono font-bold ${svc.consecutiveSuccesses > 0 ? 'text-emerald-400' : 'text-slate-300'}`}>
                          {svc.consecutiveSuccesses}/{svc.recoveryThreshold}
                        </span>
                      </div>
                    </div>
                  </td>

                  <td className="px-6 py-4 text-right">
                    <div className="flex items-center justify-end gap-1.5">
                      <button
                        onClick={() => handleManualCheck(svc.id)}
                        disabled={checkingId === svc.id}
                        title="Check now"
                        className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 disabled:opacity-50 transition"
                      >
                        <RefreshCw className={`w-3.5 h-3.5 ${checkingId === svc.id ? 'animate-spin text-indigo-400' : ''}`} />
                      </button>

                      {onSelectServiceForAnalytics && (
                        <button
                          onClick={() => onSelectServiceForAnalytics(svc.id)}
                          title="View telemetry analytics"
                          className="p-1.5 rounded-lg bg-slate-800 hover:bg-indigo-900/50 hover:text-indigo-300 text-slate-300 transition"
                        >
                          <ExternalLink className="w-3.5 h-3.5" />
                        </button>
                      )}

                      {canManageServices() && (
                        <>
                          <button
                            onClick={() => handleToggle(svc.id)}
                            title={svc.active ? 'Pause monitoring' : 'Resume monitoring'}
                            className={`p-1.5 rounded-lg transition ${
                              svc.active
                                ? 'bg-slate-800 hover:bg-amber-950/50 hover:text-amber-400 text-slate-300'
                                : 'bg-emerald-950/50 text-emerald-400 hover:bg-emerald-900/50'
                            }`}
                          >
                            {svc.active ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
                          </button>

                          <button
                            onClick={() => openEditModal(svc)}
                            title="Edit configuration"
                            className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition"
                          >
                            <Edit2 className="w-3.5 h-3.5" />
                          </button>

                          <button
                            onClick={() => handleDelete(svc.id, svc.name)}
                            title="Delete service"
                            className="p-1.5 rounded-lg bg-slate-800 hover:bg-rose-950/50 hover:text-rose-400 text-slate-300 transition"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal: Register / Edit Service */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in">
          <div className="relative w-full max-w-lg bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800">
              <h3 className="font-bold text-white">
                {editingService ? 'Edit Monitored Service' : 'Register Monitored Service'}
              </h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSave} className="p-6 space-y-4">
              {formError && (
                <div className="p-3 text-xs rounded-xl bg-rose-500/15 border border-rose-500/30 text-rose-300">
                  {formError}
                </div>
              )}

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Service / Application Name</label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500"
                  placeholder="e.g. Auth & Identity Service"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Health Check URL</label>
                <input
                  type="url"
                  required
                  value={url}
                  onChange={(e) => setUrl(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500"
                  placeholder="https://api.example.com/health"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">HTTP Method</label>
                  <select
                    value={httpMethod}
                    onChange={(e) => setHttpMethod(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500"
                  >
                    <option value="GET">GET</option>
                    <option value="POST">POST</option>
                    <option value="HEAD">HEAD</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Expected Status Code</label>
                  <input
                    type="number"
                    required
                    value={expectedStatusCode}
                    onChange={(e) => setExpectedStatusCode(parseInt(e.target.value))}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Check Interval (seconds)</label>
                  <input
                    type="number"
                    min="5"
                    max="3600"
                    required
                    value={checkIntervalSeconds}
                    onChange={(e) => setCheckIntervalSeconds(parseInt(e.target.value))}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Timeout (ms)</label>
                  <input
                    type="number"
                    min="500"
                    max="30000"
                    required
                    value={timeoutMs}
                    onChange={(e) => setTimeoutMs(parseInt(e.target.value))}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Environment</label>
                <select
                  value={environment}
                  onChange={(e) => setEnvironment(e.target.value as any)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500"
                >
                  <option value="PRODUCTION">PRODUCTION</option>
                  <option value="STAGING">STAGING</option>
                  <option value="DEVELOPMENT">DEVELOPMENT</option>
                </select>
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold rounded-xl transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold rounded-xl shadow-lg shadow-indigo-600/25 transition"
                >
                  {editingService ? 'Save Changes' : 'Register Service'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

