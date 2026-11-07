import React, { useEffect, useState } from 'react';
import { ApiClient } from '../services/api';
import { MonitoredService, ServiceAnalytics } from '../types';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import {
  BarChart3,
  TrendingUp,
  ShieldCheck,
  AlertOctagon,
  Clock,
  Layers,
  Calendar,
} from 'lucide-react';

interface AnalyticsViewProps {
  initialServiceId?: string;
}

export const AnalyticsView: React.FC<AnalyticsViewProps> = ({ initialServiceId }) => {
  const [services, setServices] = useState<MonitoredService[]>([]);
  const [selectedServiceId, setSelectedServiceId] = useState<string>(initialServiceId || '');
  const [analytics, setAnalytics] = useState<ServiceAnalytics | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    ApiClient.getServices().then((srvs) => {
      setServices(srvs);
      if (!selectedServiceId && srvs.length > 0) {
        setSelectedServiceId(srvs[0].id);
      }
    });
  }, []);

  useEffect(() => {
    if (!selectedServiceId) return;
    setLoading(true);
    ApiClient.getServiceAnalytics(selectedServiceId)
      .then((data) => {
        setAnalytics(data);
      })
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  }, [selectedServiceId]);

  const chartData = analytics?.recentChecks?.map((c) => ({
    time: new Date(c.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    latency: c.responseTimeMs,
    status: c.status,
    success: c.success,
  })) || [];

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8 animate-in fade-in duration-300">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Analytics & SLA Telemetry</h1>
          <p className="text-sm text-slate-400 mt-1">
            Historical response-time distributions, multi-window uptime calculations, and SLA compliance metrics.
          </p>
        </div>

        {/* Service Selector */}
        {services.length > 0 && (
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-slate-400">Target Endpoint:</span>
            <select
              value={selectedServiceId}
              onChange={(e) => setSelectedServiceId(e.target.value)}
              className="px-3.5 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs font-medium text-white focus:outline-none focus:border-indigo-500 shadow-sm"
            >
              {services.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name} ({s.environment})
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {loading ? (
        <div className="py-24 text-center text-slate-400">Loading telemetry data...</div>
      ) : !analytics ? (
        <div className="py-24 text-center text-slate-400">No analytics data available for this service.</div>
      ) : (
        <>
          {/* SLA Commitment Banner */}
          <div
            className={`p-6 rounded-2xl border flex flex-col md:flex-row md:items-center justify-between gap-4 shadow-xl ${
              analytics.slaCompliant
                ? 'bg-emerald-950/20 border-emerald-500/30 text-emerald-200'
                : 'bg-amber-950/20 border-amber-500/30 text-amber-200'
            }`}
          >
            <div className="flex items-center gap-3">
              <div
                className={`p-3 rounded-xl ${
                  analytics.slaCompliant ? 'bg-emerald-500/20' : 'bg-amber-500/20'
                }`}
              >
                {analytics.slaCompliant ? (
                  <ShieldCheck className="w-6 h-6 text-emerald-400" />
                ) : (
                  <AlertOctagon className="w-6 h-6 text-amber-400" />
                )}
              </div>
              <div>
                <h3 className="font-bold text-base text-white">
                  SLA Target Commitment: {analytics.slaTarget}% Availability
                </h3>
                <p className="text-xs text-slate-300 mt-0.5">
                  {analytics.slaCompliant
                    ? `Current 30-day availability is ${analytics.uptime30d}%. Service is compliant with production SLA.`
                    : `Current 30-day availability is ${analytics.uptime30d}%. Service has breached target threshold.`}
                </p>
              </div>
            </div>

            <div className="flex items-baseline gap-2 shrink-0">
              <span className="text-3xl font-extrabold text-white">{analytics.uptime30d}%</span>
              <span className="text-xs font-bold uppercase tracking-wider px-2 py-0.5 rounded bg-slate-900/80">
                {analytics.slaCompliant ? 'RESPECTED' : 'BREACHED'}
              </span>
            </div>
          </div>

          {/* Metric Cards Grid */}
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
            <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800">
              <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">24h Uptime</div>
              <div className="mt-2 text-xl font-bold text-white">{analytics.uptime24h}%</div>
              <div className="text-[10px] text-slate-500 mt-1">Rolling 24 hours</div>
            </div>

            <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800">
              <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">7-Day Uptime</div>
              <div className="mt-2 text-xl font-bold text-white">{analytics.uptime7d}%</div>
              <div className="text-[10px] text-slate-500 mt-1">Last 7 days</div>
            </div>

            <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800">
              <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">30-Day Uptime</div>
              <div className="mt-2 text-xl font-bold text-white">{analytics.uptime30d}%</div>
              <div className="text-[10px] text-slate-500 mt-1">Monthly commitment</div>
            </div>

            <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800">
              <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Avg Latency</div>
              <div className="mt-2 text-xl font-bold text-indigo-400">{analytics.avgLatency24h}ms</div>
              <div className="text-[10px] text-slate-500 mt-1">24h mean response</div>
            </div>

            <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800">
              <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Error Rate</div>
              <div className="mt-2 text-xl font-bold text-slate-200">{analytics.errorRate24h}%</div>
              <div className="text-[10px] text-slate-500 mt-1">Failed health checks</div>
            </div>

            <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800">
              <div className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Incidents (30d)</div>
              <div className="mt-2 text-xl font-bold text-rose-400">{analytics.incidents30d}</div>
              <div className="text-[10px] text-slate-500 mt-1">Outages detected</div>
            </div>
          </div>

          {/* Interactive Latency Trend Chart */}
          <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4 shadow-xl">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="font-bold text-white text-base">Latency Trend & Health History</h3>
                <p className="text-xs text-slate-400">Response time (ms) for recent scheduled health checks</p>
              </div>
              <span className="text-xs font-mono text-slate-400">N={chartData.length} data points</span>
            </div>

            <div className="h-72 w-full pt-4">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <defs>
                    <linearGradient id="latencyGradient" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#6366f1" stopOpacity={0.4} />
                      <stop offset="95%" stopColor="#6366f1" stopOpacity={0.0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                  <XAxis dataKey="time" stroke="#64748b" tick={{ fontSize: 11 }} />
                  <YAxis stroke="#64748b" tick={{ fontSize: 11 }} unit="ms" />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#0f172a',
                      borderColor: '#334155',
                      borderRadius: '0.75rem',
                      fontSize: '12px',
                    }}
                    labelStyle={{ color: '#cbd5e1', fontWeight: 'bold' }}
                  />
                  <Area
                    type="monotone"
                    dataKey="latency"
                    name="Response Time"
                    stroke="#818cf8"
                    strokeWidth={2}
                    fillOpacity={1}
                    fill="url(#latencyGradient)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

