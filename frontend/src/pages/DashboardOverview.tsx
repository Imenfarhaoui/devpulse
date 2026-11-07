import React, { useEffect, useState } from 'react';
import { ApiClient } from '../services/api';
import { realtimeService } from '../services/websocket';
import { OrgOverview, MonitoredService, Incident } from '../types';
import { useAuth } from '../context/AuthContext';
import {
  Activity,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Clock,
  ArrowUpRight,
  ShieldCheck,
  RefreshCw,
  Radio,
  ExternalLink,
  Zap,
} from 'lucide-react';

interface DashboardOverviewProps {
  onNavigateToServices: () => void;
  onNavigateToIncidents: () => void;
}

export const DashboardOverview: React.FC<DashboardOverviewProps> = ({
  onNavigateToServices,
  onNavigateToIncidents,
}) => {
  const { activeOrg, canManageIncidents } = useAuth();
  const [overview, setOverview] = useState<OrgOverview | null>(null);
  const [services, setServices] = useState<MonitoredService[]>([]);
  const [activeIncidents, setActiveIncidents] = useState<Incident[]>([]);
  const [liveChecks, setLiveChecks] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [checkingServiceId, setCheckingServiceId] = useState<string | null>(null);

  const fetchData = async () => {
    try {
      const [ov, srvs, incs] = await Promise.all([
        ApiClient.getOrgOverview(),
        ApiClient.getServices(),
        ApiClient.getIncidents('OPEN'),
      ]);
      setOverview(ov);
      setServices(srvs);
      setActiveIncidents(incs);
    } catch (err) {
      console.error('Failed to load dashboard data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();

    // Listen to real-time WebSocket check updates
    const unsubCheck = realtimeService.onServiceCheck((data) => {
      setLiveChecks((prev) => [data, ...prev.slice(0, 7)]);

      // Update service card in place
      setServices((prev) =>
        prev.map((s) =>
          s.id === data.serviceId
            ? {
                ...s,
                currentStatus: data.status,
                lastResponseTimeMs: data.responseTimeMs,
                lastCheckedAt: data.lastCheckedAt,
              }
            : s
        )
      );

      // Re-fetch overview stats periodically
      ApiClient.getOrgOverview().then(setOverview).catch(() => {});
    });

    // Listen to real-time WebSocket incident updates
    const unsubIncident = realtimeService.onIncidentUpdate((data) => {
      ApiClient.getIncidents('OPEN').then(setActiveIncidents).catch(() => {});
      ApiClient.getOrgOverview().then(setOverview).catch(() => {});
    });

    return () => {
      unsubCheck();
      unsubIncident();
    };
  }, [activeOrg?.id]);

  const handleManualCheck = async (serviceId: string) => {
    setCheckingServiceId(serviceId);
    try {
      await ApiClient.triggerCheck(serviceId);
      setTimeout(() => {
        setCheckingServiceId(null);
      }, 1200);
    } catch (e) {
      setCheckingServiceId(null);
    }
  };

  const handleAcknowledge = async (incidentId: string) => {
    try {
      await ApiClient.acknowledgeIncident(incidentId, 'Acknowledged from Overview Dashboard');
      fetchData();
    } catch (e) {
      console.error(e);
    }
  };

  const handleResolve = async (incidentId: string) => {
    try {
      await ApiClient.resolveIncident(incidentId, 'Resolved from Overview Dashboard');
      fetchData();
    } catch (e) {
      console.error(e);
    }
  };

  if (loading && !overview) {
    return (
      <div className="p-8 flex items-center justify-center min-h-[500px]">
        <div className="flex flex-col items-center gap-3">
          <RefreshCw className="w-8 h-8 text-indigo-400 animate-spin" />
          <p className="text-sm text-slate-400">Loading DevPulse telemetry...</p>
        </div>
      </div>
    );
  }

  const upCount = services.filter((s) => s.currentStatus === 'UP').length;
  const downCount = services.filter((s) => s.currentStatus === 'DOWN').length;

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8 animate-in fade-in duration-300">
      {/* Top Banner & Answers to Core Vision Questions */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-slate-900 via-indigo-950/40 to-slate-900 border border-slate-800 p-6 shadow-xl">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div>
            <div className="flex items-center gap-2 text-indigo-400 font-semibold text-xs uppercase tracking-wider mb-1">
              <Zap className="w-4 h-4 text-indigo-400" />
              <span>Unified Observability & Reliability Hub</span>
            </div>
            <h1 className="text-2xl font-bold text-white tracking-tight">
              {activeOrg?.name} Infrastructure Pulse
            </h1>
            <p className="text-sm text-slate-300 mt-1 max-w-2xl">
              Continuous failure detection across backend services, external APIs, and database replicas with automatic incident orchestration.
            </p>
          </div>

          <div className="flex items-center gap-3 shrink-0">
            <button
              onClick={fetchData}
              className="flex items-center gap-2 px-3.5 py-2 bg-slate-800/80 hover:bg-slate-800 border border-slate-700 rounded-xl text-xs font-medium text-slate-300 transition"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>Refresh Telemetry</span>
            </button>

            <button
              onClick={onNavigateToServices}
              className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-semibold shadow-lg shadow-indigo-600/25 transition"
            >
              <span>Manage Services</span>
              <ArrowUpRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      {/* KPI Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        {/* Availability 24h */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800/80 shadow-sm relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">24h Availability</span>
            <Activity className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-white tracking-tight">
              {overview?.uptime24h != null ? `${overview.uptime24h}%` : '100%'}
            </span>
          </div>
          <div className="mt-2 text-xs text-emerald-400/90 flex items-center gap-1">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>Target: 99.9%</span>
          </div>
        </div>

        {/* Avg Latency */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800/80 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">Avg Latency</span>
            <Clock className="w-4 h-4 text-indigo-400" />
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-white tracking-tight">
              {overview?.avgLatencyMs != null ? `${overview.avgLatencyMs}ms` : '0ms'}
            </span>
          </div>
          <div className="mt-2 text-xs text-slate-400">Average response time across all endpoints</div>
        </div>

        {/* Systems Healthy */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800/80 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">Active Services</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-bold text-white tracking-tight">
              {upCount}/{services.length}
            </span>
            <span className="text-xs text-emerald-400 font-medium">UP</span>
          </div>
          <div className="mt-2 text-xs text-slate-400">
            {downCount > 0 ? (
              <span className="text-rose-400 font-medium">{downCount} service(s) currently DOWN</span>
            ) : (
              'All monitored services nominal'
            )}
          </div>
        </div>

        {/* Open Incidents */}
        <div
          onClick={onNavigateToIncidents}
          className={`p-5 rounded-2xl border shadow-sm cursor-pointer transition ${
            (overview?.openIncidents || 0) > 0
              ? 'bg-rose-950/20 border-rose-500/40 hover:border-rose-500'
              : 'bg-slate-900/60 border-slate-800/80'
          }`}
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">Active Outages</span>
            <AlertTriangle
              className={`w-4 h-4 ${
                (overview?.openIncidents || 0) > 0 ? 'text-rose-400 animate-pulse' : 'text-slate-500'
              }`}
            />
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span
              className={`text-2xl font-bold tracking-tight ${
                (overview?.openIncidents || 0) > 0 ? 'text-rose-400' : 'text-white'
              }`}
            >
              {overview?.openIncidents || 0}
            </span>
          </div>
          <div className="mt-2 text-xs text-slate-400 flex items-center justify-between">
            <span>{overview?.monthlyIncidents || 0} this month</span>
            <ArrowUpRight className="w-3.5 h-3.5 text-slate-500" />
          </div>
        </div>

        {/* SLA Compliance */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800/80 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">SLA Status</span>
            <ShieldCheck className="w-4 h-4 text-indigo-400" />
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span
              className={`text-2xl font-bold tracking-tight ${
                overview?.slaCompliant !== false ? 'text-emerald-400' : 'text-amber-400'
              }`}
            >
              {overview?.slaCompliant !== false ? 'RESPECTED' : 'AT RISK'}
            </span>
          </div>
          <div className="mt-2 text-xs text-slate-400">Target commitment: 99.9% uptime</div>
        </div>
      </div>

      {/* Active Incidents Banner (if any) */}
      {activeIncidents.length > 0 && (
        <div className="p-6 rounded-2xl bg-rose-950/30 border border-rose-500/40 space-y-4 shadow-lg shadow-rose-950/20">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="relative flex h-3 w-3">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-rose-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-3 w-3 bg-rose-500"></span>
              </span>
              <h2 className="text-base font-bold text-rose-200">
                Action Required: {activeIncidents.length} Active Incident(s)
              </h2>
            </div>
            <button
              onClick={onNavigateToIncidents}
              className="text-xs font-medium text-rose-300 hover:text-white underline"
            >
              View in Incidents Hub →
            </button>
          </div>

          <div className="space-y-3">
            {activeIncidents.map((inc) => (
              <div
                key={inc.id}
                className="p-4 rounded-xl bg-slate-900/80 border border-rose-500/30 flex flex-col sm:flex-row sm:items-center justify-between gap-4"
              >
                <div>
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-rose-500/20 text-rose-300 border border-rose-500/30 uppercase">
                      {inc.severity}
                    </span>
                    <span className="text-sm font-semibold text-white">{inc.title}</span>
                  </div>
                  <p className="text-xs text-slate-400 mt-1">{inc.description}</p>
                </div>

                {canManageIncidents() && (
                  <div className="flex items-center gap-2 shrink-0">
                    {inc.status === 'OPEN' && (
                      <button
                        onClick={() => handleAcknowledge(inc.id)}
                        className="px-3 py-1.5 bg-amber-500/20 hover:bg-amber-500/30 border border-amber-500/30 text-amber-300 text-xs font-semibold rounded-lg transition"
                      >
                        Acknowledge
                      </button>
                    )}
                    <button
                      onClick={() => handleResolve(inc.id)}
                      className="px-3 py-1.5 bg-emerald-500/20 hover:bg-emerald-500/30 border border-emerald-500/30 text-emerald-300 text-xs font-semibold rounded-lg transition"
                    >
                      Resolve Incident
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Live WebSocket Check Stream */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Radio className="w-4 h-4 text-emerald-400 animate-pulse" />
            <h3 className="text-sm font-bold text-white uppercase tracking-wider">
              Live Health Checks Feed (WebSocket STOMP)
            </h3>
          </div>
          <span className="text-xs text-slate-400 font-mono">Auto-polling via Spring Scheduler</span>
        </div>

        {liveChecks.length === 0 ? (
          <div className="py-4 text-center text-xs text-slate-500">
            Waiting for live check broadcast from monitoring engine...
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-2.5">
            {liveChecks.map((check, idx) => (
              <div
                key={idx}
                className="p-3 rounded-xl bg-slate-950/80 border border-slate-800/80 flex items-center justify-between text-xs animate-in fade-in"
              >
                <div className="truncate mr-2">
                  <div className="font-semibold text-slate-200 truncate">{check.serviceName}</div>
                  <div className="text-[10px] text-slate-500 font-mono">
                    HTTP {check.httpStatus} • {check.responseTimeMs}ms
                  </div>
                </div>
                <span
                  className={`px-2 py-0.5 rounded text-[10px] font-bold shrink-0 ${
                    check.success
                      ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                      : 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
                  }`}
                >
                  {check.success ? 'PASSED' : 'FAILED'}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Monitored Services Quick Overview */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-bold text-white">Monitored Endpoints Status</h3>
            <p className="text-xs text-slate-400">Applications, External APIs, and Infrastructure Services</p>
          </div>
          <button
            onClick={onNavigateToServices}
            className="text-xs font-semibold text-indigo-400 hover:text-indigo-300 flex items-center gap-1"
          >
            <span>View All ({services.length})</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {services.map((svc) => (
            <div
              key={svc.id}
              className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 hover:border-slate-700 transition flex flex-col justify-between space-y-4"
            >
              <div>
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 uppercase tracking-wide">
                      {svc.environment}
                    </span>
                    <h4 className="font-semibold text-white mt-1 text-base">{svc.name}</h4>
                  </div>
                  <span
                    className={`px-2.5 py-1 rounded-full text-xs font-bold shrink-0 ${
                      svc.currentStatus === 'UP'
                        ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                        : svc.currentStatus === 'DOWN'
                        ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                        : svc.currentStatus === 'DEGRADED'
                        ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                        : 'bg-slate-800 text-slate-400 border border-slate-700'
                    }`}
                  >
                    ● {svc.currentStatus}
                  </span>
                </div>

                <div className="mt-2 text-xs text-slate-400 font-mono truncate" title={svc.url}>
                  {svc.httpMethod} {svc.url}
                </div>
              </div>

              {/* Telemetry snippet */}
              <div className="pt-3 border-t border-slate-800/80 flex items-center justify-between text-xs">
                <div>
                  <span className="text-slate-400">Response: </span>
                  <span className="font-semibold text-slate-200">
                    {svc.lastResponseTimeMs != null ? `${svc.lastResponseTimeMs}ms` : '—'}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <span className="text-[11px] text-slate-400">every {svc.checkIntervalSeconds}s</span>
                  <button
                    onClick={() => handleManualCheck(svc.id)}
                    disabled={checkingServiceId === svc.id}
                    title="Run health check right now"
                    className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 disabled:opacity-50 transition"
                  >
                    <RefreshCw className={`w-3.5 h-3.5 ${checkingServiceId === svc.id ? 'animate-spin text-indigo-400' : ''}`} />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

