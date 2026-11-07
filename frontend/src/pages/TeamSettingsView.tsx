import React, { useEffect, useState } from 'react';
import { ApiClient } from '../services/api';
import { TeamMember, Invitation, UserRole } from '../types';
import { useAuth } from '../context/AuthContext';
import {
  Users2,
  UserPlus,
  Mail,
  Shield,
  Trash2,
  X,
  Building,
  CheckCircle,
} from 'lucide-react';

export const TeamSettingsView: React.FC = () => {
  const { activeOrg, user, canManageTeam } = useAuth();
  const [members, setMembers] = useState<TeamMember[]>([]);
  const [invitations, setInvitations] = useState<Invitation[]>([]);
  const [loading, setLoading] = useState(true);
  const [isInviteOpen, setIsInviteOpen] = useState(false);

  // Invite Form
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState<UserRole>('DEVELOPER');
  const [inviteError, setInviteError] = useState('');
  const [inviteSuccess, setInviteSuccess] = useState('');

  const fetchData = async () => {
    if (!activeOrg?.id) return;
    try {
      const [m, invs] = await Promise.all([
        ApiClient.getOrgMembers(activeOrg.id),
        ApiClient.getInvitations(activeOrg.id),
      ]);
      setMembers(m);
      setInvitations(invs);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [activeOrg?.id]);

  const handleRoleChange = async (targetUserId: string, newRole: string) => {
    if (!activeOrg?.id) return;
    try {
      await ApiClient.updateMemberRole(activeOrg.id, targetUserId, newRole);
      fetchData();
    } catch (e: any) {
      alert(e.message || 'Failed to update role');
    }
  };

  const handleRemoveMember = async (targetUserId: string, name: string) => {
    if (!activeOrg?.id) return;
    if (!window.confirm(`Remove ${name} from ${activeOrg.name}?`)) return;
    try {
      await ApiClient.removeMember(activeOrg.id, targetUserId);
      fetchData();
    } catch (e: any) {
      alert(e.message || 'Failed to remove member');
    }
  };

  const handleInviteSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeOrg?.id) return;
    setInviteError('');
    setInviteSuccess('');
    try {
      await ApiClient.inviteMember(activeOrg.id, inviteEmail, inviteRole);
      setInviteSuccess(`Invitation token generated and sent to ${inviteEmail}!`);
      setInviteEmail('');
      fetchData();
      setTimeout(() => {
        setIsInviteOpen(false);
        setInviteSuccess('');
      }, 1500);
    } catch (err: any) {
      setInviteError(err.message || 'Failed to send invitation');
    }
  };

  const handleRevokeInvitation = async (invitationId: string) => {
    if (!activeOrg?.id) return;
    try {
      await ApiClient.revokeInvitation(activeOrg.id, invitationId);
      fetchData();
    } catch (e) {
      console.error(e);
    }
  };

  const getRoleBadge = (r: string) => {
    switch (r) {
      case 'ORG_OWNER': return 'bg-amber-500/20 text-amber-300 border-amber-500/30';
      case 'DEVELOPER': return 'bg-indigo-500/20 text-indigo-300 border-indigo-500/30';
      case 'INCIDENT_MANAGER': return 'bg-rose-500/20 text-rose-300 border-rose-500/30';
      case 'VIEWER': return 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30';
      default: return 'bg-slate-700 text-slate-300 border-slate-600';
    }
  };

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8 animate-in fade-in duration-300">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Organization & RBAC Settings</h1>
          <p className="text-sm text-slate-400 mt-1">
            Manage company memberships, permissions, invitations, and data isolation controls.
          </p>
        </div>

        {canManageTeam() && (
          <button
            onClick={() => setIsInviteOpen(true)}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-sm font-semibold shadow-lg shadow-indigo-600/25 transition shrink-0"
          >
            <UserPlus className="w-4 h-4" />
            <span>Invite Team Member</span>
          </button>
        )}
      </div>

      {/* Organization Meta Card */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="p-3.5 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
            <Building className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-lg font-bold text-white">{activeOrg?.name}</h2>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                {activeOrg?.plan} PLAN
              </span>
            </div>
            <p className="text-xs text-slate-400 font-mono mt-0.5">slug: {activeOrg?.slug} • UUID: {activeOrg?.id}</p>
          </div>
        </div>
      </div>

      {/* Members Table */}
      <div className="space-y-3">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider flex items-center gap-2">
          <Users2 className="w-4 h-4 text-indigo-400" />
          <span>Members & Assigned Permissions</span>
        </h3>

        {loading ? (
          <div className="py-10 text-center text-slate-400">Loading members...</div>
        ) : (
          <div className="overflow-hidden rounded-2xl border border-slate-800 bg-slate-900/60 shadow-xl">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-950/60 border-b border-slate-800 text-xs text-slate-400 uppercase tracking-wider">
                <tr>
                  <th className="px-6 py-4 font-semibold">User</th>
                  <th className="px-4 py-4 font-semibold">Email</th>
                  <th className="px-4 py-4 font-semibold">Assigned Role</th>
                  <th className="px-4 py-4 font-semibold">Joined Date</th>
                  <th className="px-6 py-4 font-semibold text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80">
                {members.map((m) => {
                  const isSelf = m.userId === user?.id;
                  return (
                    <tr key={m.membershipId} className="hover:bg-slate-800/30 transition">
                      <td className="px-6 py-4 font-semibold text-white">
                        <div className="flex items-center gap-2">
                          <span>{m.firstName} {m.lastName}</span>
                          {isSelf && (
                            <span className="text-[10px] px-1.5 py-0.2 rounded bg-slate-800 text-slate-400 font-normal">
                              You
                            </span>
                          )}
                        </div>
                      </td>

                      <td className="px-4 py-4 text-slate-300 font-mono text-xs">{m.email}</td>

                      <td className="px-4 py-4">
                        {canManageTeam() && !isSelf ? (
                          <select
                            value={m.role}
                            onChange={(e) => handleRoleChange(m.userId, e.target.value)}
                            className="px-2.5 py-1 bg-slate-950 border border-slate-700 rounded-lg text-xs font-semibold text-white focus:outline-none focus:border-indigo-500"
                          >
                            <option value="ORG_OWNER">ORG_OWNER</option>
                            <option value="DEVELOPER">DEVELOPER</option>
                            <option value="INCIDENT_MANAGER">INCIDENT_MANAGER</option>
                            <option value="VIEWER">VIEWER</option>
                          </select>
                        ) : (
                          <span className={`px-2.5 py-1 rounded-full text-xs font-bold border ${getRoleBadge(m.role)}`}>
                            {m.role}
                          </span>
                        )}
                      </td>

                      <td className="px-4 py-4 text-xs text-slate-400">
                        {new Date(m.joinedAt).toLocaleDateString()}
                      </td>

                      <td className="px-6 py-4 text-right">
                        {canManageTeam() && !isSelf && (
                          <button
                            onClick={() => handleRemoveMember(m.userId, `${m.firstName} ${m.lastName}`)}
                            title="Remove member"
                            className="p-1.5 rounded-lg bg-slate-800 hover:bg-rose-950/60 hover:text-rose-400 text-slate-300 transition"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
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

      {/* Invitations Table */}
      {invitations.length > 0 && (
        <div className="space-y-3">
          <h3 className="text-sm font-bold text-white uppercase tracking-wider flex items-center gap-2">
            <Mail className="w-4 h-4 text-indigo-400" />
            <span>Pending Organization Invitations</span>
          </h3>

          <div className="overflow-hidden rounded-2xl border border-slate-800 bg-slate-900/60 shadow-xl">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-950/60 border-b border-slate-800 text-xs text-slate-400 uppercase tracking-wider">
                <tr>
                  <th className="px-6 py-3 font-semibold">Invited Email</th>
                  <th className="px-4 py-3 font-semibold">Designated Role</th>
                  <th className="px-4 py-3 font-semibold">Token</th>
                  <th className="px-4 py-3 font-semibold">Expires</th>
                  <th className="px-6 py-3 font-semibold text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/80 text-xs">
                {invitations.map((inv) => (
                  <tr key={inv.id} className="hover:bg-slate-800/20">
                    <td className="px-6 py-3 font-medium text-white">{inv.email}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold border ${getRoleBadge(inv.role)}`}>
                        {inv.role}
                      </span>
                    </td>
                    <td className="px-4 py-3 font-mono text-slate-500 truncate max-w-xs">{inv.token}</td>
                    <td className="px-4 py-3 text-slate-400">{new Date(inv.expiresAt).toLocaleDateString()}</td>
                    <td className="px-6 py-3 text-right">
                      {canManageTeam() && inv.status === 'PENDING' && (
                        <button
                          onClick={() => handleRevokeInvitation(inv.id)}
                          className="px-2.5 py-1 text-xs text-rose-400 hover:bg-rose-950/40 rounded-lg transition"
                        >
                          Revoke
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal: Invite Team Member */}
      {isInviteOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in">
          <div className="relative w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800">
              <h3 className="font-bold text-white">Invite Team Member</h3>
              <button
                onClick={() => setIsInviteOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleInviteSubmit} className="p-6 space-y-4">
              {inviteError && (
                <div className="p-3 text-xs rounded-xl bg-rose-500/15 border border-rose-500/30 text-rose-300">
                  {inviteError}
                </div>
              )}

              {inviteSuccess && (
                <div className="p-3 text-xs rounded-xl bg-emerald-500/15 border border-emerald-500/30 text-emerald-300 flex items-center gap-2">
                  <CheckCircle className="w-4 h-4" />
                  <span>{inviteSuccess}</span>
                </div>
              )}

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Colleague Work Email</label>
                <input
                  type="email"
                  required
                  value={inviteEmail}
                  onChange={(e) => setInviteEmail(e.target.value)}
                  placeholder="teammate@company.com"
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Assigned RBAC Role</label>
                <select
                  value={inviteRole}
                  onChange={(e) => setInviteRole(e.target.value as any)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500"
                >
                  <option value="DEVELOPER">Developer (Register & Monitor Applications)</option>
                  <option value="INCIDENT_MANAGER">Incident Manager (Acknowledge & Resolve Incidents)</option>
                  <option value="VIEWER">Viewer (Read-only access to Dashboards)</option>
                  <option value="ORG_OWNER">Organization Owner (Full Admin Permissions)</option>
                </select>
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsInviteOpen(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold rounded-xl shadow-lg shadow-indigo-600/25"
                >
                  Send Invitation
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

