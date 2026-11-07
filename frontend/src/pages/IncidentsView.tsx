import React, { useEffect, useState } from 'react';
import { ApiClient } from '../services/api';
import { Incident, IncidentEvent, TeamMember, MonitoredService } from '../types';
import { useAuth } from '../context/AuthContext';
import {
  AlertTriangle,
  CheckCircle2,
  Clock,
  User as UserIcon,
  MessageSquare,
  ArrowRight,
  Send,
  X,
  Radio,
  RefreshCw,
} from 'lucide-react';

export const IncidentsView: React.FC = () => {
  const { activeOrg, canManageIncidents } = useAuth();
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [services, setServices] = useState<Record<string, MonitoredService>>({});
  const [teamMembers, setTeamMembers] = useState<TeamMember[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [loading, setLoading] = useState(true);

  // Selected incident details
  const [selectedIncident, setSelectedIncident] = useState<Incident | null>(null);
  const [events, setEvents] = useState<IncidentEvent[]>([]);
  const [commentText, setCommentText] = useState('');
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [isResolving, setIsResolving] = useState(false);
  const [isDetailOpen, setIsDetailOpen] = useState(false);

  const fetchIncidents = async () => {
    try {
      const [incs, srvs] = await Promise.all([
        ApiClient.getIncidents(statusFilter),
        ApiClient.getServices(),
      ]);
      setIncidents(incs);
      const srvMap: Record<string, MonitoredService> = {};
      srvs.forEach((s: MonitoredService) => {
        srvMap[s.id] = s;
      });
      setServices(srvMap);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const fetchTeam = async () => {
    if (activeOrg?.id) {
      try {
        const members = await ApiClient.getOrgMembers(activeOrg.id);
        setTeamMembers(members);
      } catch (e) {
        console.error(e);
      }
    }
  };

  useEffect(() => {
    fetchIncidents();
    fetchTeam();
  }, [statusFilter, activeOrg?.id]);

  const openIncidentDetail = async (inc: Incident) => {
    setSelectedIncident(inc);
    setIsResolving(false);
    setResolutionNotes('');
    setIsDetailOpen(true);
    try {
      const evs = await ApiClient.getIncidentEvents(inc.id);
      setEvents(evs);
    } catch (err) {
      console.error(err);
    }
  };

  const handleAcknowledge = async () => {
    if (!selectedIncident) return;
    try {
      const updated = await ApiClient.acknowledgeIncident(selectedIncident.id, 'Acknowledged by incident manager');
      setSelectedIncident(updated);
      const evs = await ApiClient.getIncidentEvents(selectedIncident.id);
      setEvents(evs);
      fetchIncidents();
    } catch (err) {
      console.error(err);
    }
  };

  const handleResolveSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedIncident) return;
    try {
      const updated = await ApiClient.resolveIncident(selectedIncident.id, resolutionNotes);
      setSelectedIncident(updated);
      setIsResolving(false);
      const evs = await ApiClient.getIncidentEvents(selectedIncident.id);
      setEvents(evs);
      fetchIncidents();
    } catch (err) {
      console.error(err);
    }
  };

  const handleReopen = async () => {
    if (!selectedIncident) return;
    const reason = window.prompt('Reason for reopening this incident:');
    if (!reason) return;
    try {
      const updated = await ApiClient.reopenIncident(selectedIncident.id, reason);
      setSelectedIncident(updated);
      const evs = await ApiClient.getIncidentEvents(selectedIncident.id);
      setEvents(evs);
      fetchIncidents();
    } catch (err) {
      console.error(err);
    }
  };

  const handleAssign = async (userId: string) => {
    if (!selectedIncident || !userId) return;
    try {
      const updated = await ApiClient.assignIncident(selectedIncident.id, userId);
      setSelectedIncident(updated);
      const evs = await ApiClient.getIncidentEvents(selectedIncident.id);
      setEvents(evs);
      fetchIncidents();
    } catch (err) {
      console.error(err);
    }
  };

  const handleAddComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedIncident || !commentText.trim()) return;
    try {
      await ApiClient.addIncidentComment(selectedIncident.id, commentText.trim());
      setCommentText('');
      const evs = await ApiClient.getIncidentEvents(selectedIncident.id);
      setEvents(evs);
    } catch (err) {
      console.error(err);
    }
  };

  const getSeverityBadge = (sev: string) => {
    switch (sev) {
      case 'CRITICAL': return 'bg-rose-500/20 text-rose-300 border-rose-500/30';
      case 'HIGH': return 'bg-orange-500/20 text-orange-300 border-orange-500/30';
      case 'MEDIUM': return 'bg-amber-500/20 text-amber-300 border-amber-500/30';
      default: return 'bg-slate-700 text-slate-300 border-slate-600';
    }
  };

  const getStatusBadge = (st: string) => {
    switch (st) {
      case 'OPEN': return 'bg-rose-500/20 text-rose-400 border-rose-500/30 animate-pulse';
      case 'ACKNOWLEDGED': return 'bg-amber-500/20 text-amber-400 border-amber-500/30';
      case 'RESOLVED': return 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30';
      case 'REOPENED': return 'bg-purple-500/20 text-purple-400 border-purple-500/30';
      default: return 'bg-slate-800 text-slate-400 border-slate-700';
    }
  };

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-6 animate-in fade-in duration-300">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Incidents & Outage Response</h1>
          <p className="text-sm text-slate-400 mt-1">
            Automated detection after 3 consecutive check failures, lifecycle assignment, and recovery audit trails.
          </p>
        </div>

        <button
          onClick={fetchIncidents}
          className="flex items-center gap-2 px-3.5 py-2 bg-slate-800/80 hover:bg-slate-800 border border-slate-700 rounded-xl text-xs font-medium text-slate-300 transition shrink-0"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          <span>Refresh Incidents</span>
        </button>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-800 pb-4">
        {['ALL', 'OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'REOPENED'].map((st) => (
          <button
            key={st}
            onClick={() => setStatusFilter(st)}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition ${
              statusFilter === st
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'bg-slate-900 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
            }`}
          >
            {st}
          </button>
        ))}
      </div>

      {/* Incidents List */}
      {loading ? (
        <div className="py-20 text-center text-slate-400">Loading incidents...</div>
      ) : incidents.length === 0 ? (
        <div className="p-12 text-center rounded-2xl bg-slate-900/40 border border-slate-800 space-y-3">
          <CheckCircle2 className="w-10 h-10 text-emerald-500 mx-auto" />
          <h3 className="text-base font-semibold text-slate-300">No Incidents Found</h3>
          <p className="text-xs text-slate-400 max-w-md mx-auto">
            {statusFilter !== 'ALL'
              ? `There are no incidents currently in "${statusFilter}" status.`
              : 'All monitored applications and APIs are passing health checks without detected outages.'}
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {incidents.map((inc) => {
            const svc = services[inc.serviceId];
            return (
              <div
                key={inc.id}
                onClick={() => openIncidentDetail(inc)}
                className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 hover:border-slate-700 cursor-pointer transition flex flex-col md:flex-row md:items-center justify-between gap-4 group"
              >
                <div className="space-y-1.5">
                  <div className="flex items-center gap-2.5">
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold border uppercase ${getSeverityBadge(inc.severity)}`}>
                      {inc.severity}
                    </span>
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold border uppercase ${getStatusBadge(inc.status)}`}>
                      {inc.status}
                    </span>
                    <h3 className="font-semibold text-white group-hover:text-indigo-300 transition">
                      {inc.title}
                    </h3>
                  </div>

                  <p className="text-xs text-slate-400 line-clamp-1">{inc.description}</p>

                  <div className="flex items-center gap-4 text-[11px] text-slate-400 pt-1">
                    <span className="font-medium text-slate-300">
                      Service: <span className="text-indigo-400 font-semibold">{svc?.name || inc.serviceId}</span>
                    </span>
                    <span className="flex items-center gap-1">
                      <Clock className="w-3 h-3 text-slate-500" />
                      Started {new Date(inc.startedAt).toLocaleString()}
                    </span>
                    {inc.resolvedAt && (
                      <span className="text-emerald-400">
                        Resolved {new Date(inc.resolvedAt).toLocaleTimeString()}
                      </span>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-3 shrink-0">
                  {inc.assignedTo ? (
                    <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-800 text-xs text-slate-300">
                      <UserIcon className="w-3.5 h-3.5 text-indigo-400" />
                      <span>{inc.assignedTo.firstName} {inc.assignedTo.lastName}</span>
                    </div>
                  ) : (
                    <span className="text-xs text-slate-400 italic">Unassigned</span>
                  )}
                  <ArrowRight className="w-4 h-4 text-slate-600 group-hover:text-indigo-400 group-hover:translate-x-1 transition" />
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Incident Detail Drawer / Modal */}
      {isDetailOpen && selectedIncident && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in">
          <div className="relative w-full max-w-2xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl flex flex-col max-h-[90vh] overflow-hidden">
            {/* Header */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-950/60">
              <div className="flex items-center gap-2">
                <span className={`px-2 py-0.5 rounded text-[10px] font-bold border uppercase ${getSeverityBadge(selectedIncident.severity)}`}>
                  {selectedIncident.severity}
                </span>
                <span className={`px-2 py-0.5 rounded text-[10px] font-bold border uppercase ${getStatusBadge(selectedIncident.status)}`}>
                  {selectedIncident.status}
                </span>
                <h3 className="font-bold text-white text-base truncate max-w-md">{selectedIncident.title}</h3>
              </div>
              <button
                onClick={() => setIsDetailOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Body */}
            <div className="p-6 overflow-y-auto space-y-6">
              {/* Description & Timing */}
              <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800 space-y-2 text-xs">
                <div className="text-slate-300">{selectedIncident.description}</div>
                <div className="grid grid-cols-2 gap-2 text-slate-400 pt-2 border-t border-slate-800/80">
                  <div>
                    <span className="font-semibold text-slate-300">Started: </span>
                    {new Date(selectedIncident.startedAt).toLocaleString()}
                  </div>
                  <div>
                    <span className="font-semibold text-slate-300">Target Service: </span>
                    <span className="text-indigo-400 font-medium">
                      {services[selectedIncident.serviceId]?.name || selectedIncident.serviceId}
                    </span>
                  </div>
                </div>
              </div>

              {/* Assignment & State Actions */}
              {canManageIncidents() && (
                <div className="p-4 rounded-xl bg-slate-800/40 border border-slate-800 space-y-3">
                  <div className="text-xs font-semibold text-slate-300 uppercase tracking-wider">
                    Incident Response & State Machine
                  </div>

                  <div className="flex flex-wrap items-center gap-3">
                    {/* Assignee select */}
                    <div className="flex items-center gap-2">
                      <span className="text-xs text-slate-400">Assign:</span>
                      <select
                        value={selectedIncident.assignedTo?.id || ''}
                        onChange={(e) => handleAssign(e.target.value)}
                        className="px-2.5 py-1.5 bg-slate-950 border border-slate-700 rounded-lg text-xs text-white focus:outline-none focus:border-indigo-500"
                      >
                        <option value="">Select Assignee</option>
                        {teamMembers.map((m) => (
                          <option key={m.userId} value={m.userId}>
                            {m.firstName} {m.lastName} ({m.role})
                          </option>
                        ))}
                      </select>
                    </div>

                    {/* Action buttons */}
                    <div className="flex items-center gap-2 ml-auto">
                      {selectedIncident.status === 'OPEN' && (
                        <button
                          onClick={handleAcknowledge}
                          className="px-3 py-1.5 bg-amber-500 hover:bg-amber-400 text-slate-950 text-xs font-bold rounded-lg transition"
                        >
                          Acknowledge Incident
                        </button>
                      )}

                      {selectedIncident.status !== 'RESOLVED' && !isResolving && (
                        <button
                          onClick={() => setIsResolving(true)}
                          className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-lg transition"
                        >
                          Mark as Resolved
                        </button>
                      )}

                      {selectedIncident.status === 'RESOLVED' && (
                        <button
                          onClick={handleReopen}
                          className="px-3 py-1.5 bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold rounded-lg transition"
                        >
                          Reopen Incident
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Resolve Note Form */}
                  {isResolving && (
                    <form onSubmit={handleResolveSubmit} className="pt-3 border-t border-slate-800 space-y-2">
                      <label className="block text-xs font-medium text-slate-300">
                        Resolution Notes / Root Cause Analysis
                      </label>
                      <textarea
                        required
                        rows={2}
                        value={resolutionNotes}
                        onChange={(e) => setResolutionNotes(e.target.value)}
                        placeholder="Explain resolution steps, root cause, or recovery confirmation..."
                        className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-lg text-xs text-white focus:outline-none focus:border-indigo-500"
                      />
                      <div className="flex justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => setIsResolving(false)}
                          className="px-3 py-1 bg-slate-800 text-slate-300 text-xs rounded-lg"
                        >
                          Cancel
                        </button>
                        <button
                          type="submit"
                          className="px-3 py-1 bg-emerald-600 text-white text-xs font-bold rounded-lg"
                        >
                          Confirm Resolution
                        </button>
                      </div>
                    </form>
                  )}
                </div>
              )}

              {/* Lifecycle Events & Comments Audit Trail */}
              <div className="space-y-3">
                <div className="flex items-center gap-1.5 text-xs font-bold text-slate-300 uppercase tracking-wider">
                  <Clock className="w-3.5 h-3.5 text-indigo-400" />
                  <span>Lifecycle Events & Audit Trail</span>
                </div>

                <div className="space-y-2.5 max-h-56 overflow-y-auto pr-1">
                  {events.length === 0 ? (
                    <div className="text-xs text-slate-500 italic">No events recorded.</div>
                  ) : (
                    events.map((ev) => (
                      <div
                        key={ev.id}
                        className="p-3 rounded-xl bg-slate-950/60 border border-slate-800/80 text-xs space-y-1"
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <span className="font-semibold text-indigo-300">
                              {ev.user ? `${ev.user.firstName} ${ev.user.lastName}` : 'DevPulse Bot'}
                            </span>
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-slate-800 text-slate-400 uppercase">
                              {ev.eventType}
                            </span>
                          </div>
                          <span className="text-[10px] text-slate-500">
                            {new Date(ev.createdAt).toLocaleTimeString()}
                          </span>
                        </div>
                        <p className="text-slate-300">{ev.comment}</p>
                      </div>
                    ))
                  )}
                </div>

                {/* Add Comment */}
                <form onSubmit={handleAddComment} className="flex gap-2 pt-2">
                  <input
                    type="text"
                    value={commentText}
                    onChange={(e) => setCommentText(e.target.value)}
                    placeholder="Add an update note or comment to this incident..."
                    className="flex-1 px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white focus:outline-none focus:border-indigo-500"
                  />
                  <button
                    type="submit"
                    disabled={!commentText.trim()}
                    className="px-3 py-2 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 text-white rounded-xl transition"
                  >
                    <Send className="w-3.5 h-3.5" />
                  </button>
                </form>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

