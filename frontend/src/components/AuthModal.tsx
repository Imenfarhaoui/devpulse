import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { X, Shield, UserCheck, Lock, Mail, Building, Key } from 'lucide-react';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const AuthModal: React.FC<AuthModalProps> = ({ isOpen, onClose }) => {
  const { login, register } = useAuth();
  const [tab, setTab] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('owner@acme.corp');
  const [password, setPassword] = useState('password123');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [orgName, setOrgName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (tab === 'login') {
        await login(email, password);
      } else {
        await register({ firstName, lastName, email, password, organizationName: orgName });
      }
      onClose();
    } catch (err: any) {
      setError(err.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  const handleDemoLogin = async (demoEmail: string) => {
    setError('');
    setLoading(true);
    try {
      await login(demoEmail, 'password123');
      onClose();
    } catch (err: any) {
      setError(err.message || 'Demo login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative w-full max-w-lg bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-900/60">
          <div className="flex items-center gap-2">
            <Shield className="w-5 h-5 text-indigo-400" />
            <h2 className="text-lg font-bold text-white">DevPulse Authentication & RBAC</h2>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* 1-Click Role Switcher */}
        <div className="p-6 bg-slate-950/60 border-b border-slate-800">
          <div className="text-xs font-semibold text-indigo-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
            <Key className="w-3.5 h-3.5" />
            <span>1-Click Demo Persona Switcher (RBAC)</span>
          </div>
          <p className="text-xs text-slate-400 mb-3">
            Select any persona to immediately experience DevPulse under that exact role and permission scope:
          </p>

          <div className="grid grid-cols-2 gap-2 text-xs">
            <button
              onClick={() => handleDemoLogin('owner@acme.corp')}
              className="p-2.5 rounded-xl border border-amber-500/30 bg-amber-500/10 hover:bg-amber-500/20 text-left transition group"
            >
              <div className="font-semibold text-amber-300 flex items-center justify-between">
                <span>Alex Vance</span>
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-amber-500/20 text-amber-300">OWNER</span>
              </div>
              <div className="text-[11px] text-slate-400 truncate mt-0.5">Full org & user control</div>
            </button>

            <button
              onClick={() => handleDemoLogin('dev@acme.corp')}
              className="p-2.5 rounded-xl border border-indigo-500/30 bg-indigo-500/10 hover:bg-indigo-500/20 text-left transition group"
            >
              <div className="font-semibold text-indigo-300 flex items-center justify-between">
                <span>Sarah Connor</span>
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-indigo-500/20 text-indigo-300">DEVELOPER</span>
              </div>
              <div className="text-[11px] text-slate-400 truncate mt-0.5">Register & manage services</div>
            </button>

            <button
              onClick={() => handleDemoLogin('incident.mgr@acme.corp')}
              className="p-2.5 rounded-xl border border-rose-500/30 bg-rose-500/10 hover:bg-rose-500/20 text-left transition group"
            >
              <div className="font-semibold text-rose-300 flex items-center justify-between">
                <span>Gordon Freeman</span>
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-rose-500/20 text-rose-300">INCIDENT MGR</span>
              </div>
              <div className="text-[11px] text-slate-400 truncate mt-0.5">Ack & resolve outages</div>
            </button>

            <button
              onClick={() => handleDemoLogin('viewer@acme.corp')}
              className="p-2.5 rounded-xl border border-emerald-500/30 bg-emerald-500/10 hover:bg-emerald-500/20 text-left transition group"
            >
              <div className="font-semibold text-emerald-300 flex items-center justify-between">
                <span>Ellie Williams</span>
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-300">VIEWER</span>
              </div>
              <div className="text-[11px] text-slate-400 truncate mt-0.5">Read-only dashboards</div>
            </button>
          </div>

          <button
            onClick={() => handleDemoLogin('admin@devpulse.io')}
            className="w-full mt-2 p-2 rounded-xl border border-purple-500/30 bg-purple-500/10 hover:bg-purple-500/20 text-xs font-semibold text-purple-300 flex items-center justify-between px-3 transition"
          >
            <span className="flex items-center gap-1.5">
              <UserCheck className="w-3.5 h-3.5" />
              <span>Platform Administrator</span>
            </span>
            <span className="text-[10px] px-1.5 py-0.5 rounded bg-purple-500/20 uppercase tracking-wide">
              Global Platform Scope
            </span>
          </button>
        </div>

        {/* Tab selector */}
        <div className="flex border-b border-slate-800">
          <button
            onClick={() => setTab('login')}
            className={`flex-1 py-3 text-sm font-semibold transition ${
              tab === 'login'
                ? 'text-indigo-400 border-b-2 border-indigo-500 bg-slate-800/20'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Sign In
          </button>
          <button
            onClick={() => setTab('register')}
            className={`flex-1 py-3 text-sm font-semibold transition ${
              tab === 'register'
                ? 'text-indigo-400 border-b-2 border-indigo-500 bg-slate-800/20'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Create Organization Account
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 text-xs rounded-xl bg-rose-500/15 border border-rose-500/30 text-rose-300">
              {error}
            </div>
          )}

          {tab === 'register' && (
            <>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">First Name</label>
                  <input
                    type="text"
                    required
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500"
                    placeholder="Alex"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Last Name</label>
                  <input
                    type="text"
                    required
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500"
                    placeholder="Vance"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Company / Organization Name</label>
                <div className="relative">
                  <Building className="absolute left-3 top-2.5 w-4 h-4 text-slate-500" />
                  <input
                    type="text"
                    value={orgName}
                    onChange={(e) => setOrgName(e.target.value)}
                    className="w-full pl-9 pr-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500"
                    placeholder="Acme Corporation"
                  />
                </div>
              </div>
            </>
          )}

          <div>
            <label className="block text-xs font-medium text-slate-400 mb-1">Work Email</label>
            <div className="relative">
              <Mail className="absolute left-3 top-2.5 w-4 h-4 text-slate-500" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full pl-9 pr-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500"
                placeholder="developer@company.com"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-400 mb-1">Password</label>
            <div className="relative">
              <Lock className="absolute left-3 top-2.5 w-4 h-4 text-slate-500" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full pl-9 pr-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white focus:outline-none focus:border-indigo-500"
                placeholder="••••••••"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 px-4 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white text-sm font-semibold rounded-xl shadow-lg shadow-indigo-600/25 transition"
          >
            {loading ? 'Please wait...' : tab === 'login' ? 'Sign In to DevPulse' : 'Create Account & Organization'}
          </button>
        </form>
      </div>
    </div>
  );
};

