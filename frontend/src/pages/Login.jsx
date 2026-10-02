import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ShieldCheck, Loader2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { apiErrorMessage } from '../services/api';
import { notify } from '../components/Toast';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await login(form.username.trim(), form.password);
      notify.success('Welcome back.');
      navigate(location.state?.from?.pathname || '/dashboard', { replace: true });
    } catch (err) {
      setError(apiErrorMessage(err, 'Invalid username or password.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-navy-950 px-4">
      <div className="w-full max-w-md animate-slide-up">
        <div className="mb-8 flex flex-col items-center text-center">
          <div className="mb-3 flex h-14 w-14 items-center justify-center rounded-2xl bg-accent-600">
            <ShieldCheck className="text-white" size={28} />
          </div>
          <h1 className="text-2xl font-bold text-white">CaseLock</h1>
          <p className="mt-1 text-sm text-navy-300">Secure Digital Evidence Management</p>
        </div>

        <form onSubmit={handleSubmit} className="card space-y-4 p-6">
          {error && (
            <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
              {error}
            </div>
          )}

          <div>
            <label htmlFor="username" className="label">
              Username
            </label>
            <input
              id="username"
              type="text"
              required
              autoFocus
              className="input"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              placeholder="e.g. investigator"
            />
          </div>

          <div>
            <label htmlFor="password" className="label">
              Password
            </label>
            <input
              id="password"
              type="password"
              required
              className="input"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              placeholder="********"
            />
          </div>

          <button type="submit" className="btn-accent w-full" disabled={submitting}>
            {submitting ? <Loader2 size={16} className="animate-spin" /> : null}
            {submitting ? 'Signing in...' : 'Sign in'}
          </button>

          <p className="text-center text-sm text-slate-500">
            Don&apos;t have an account?{' '}
            <Link to="/register" className="font-medium text-accent-600 hover:underline">
              Register
            </Link>
          </p>
        </form>

        <div className="mt-4 rounded-xl border border-navy-800 bg-navy-900/90 p-4 text-xs text-navy-200">
          <p className="mb-2 font-semibold text-white">⚡ Quick Role Switch (For Presentation & Demo)</p>
          <div className="grid grid-cols-1 gap-1.5 sm:grid-cols-2">
            {[
              { label: '👮 Station Chief / Admin', u: 'admin', p: 'Admin123!' },
              { label: '🕵️ Lead Investigator', u: 'investigator', p: 'Investigate123!' },
              { label: '🔬 Forensic Analyst', u: 'analyst', p: 'Analyze123!' },
              { label: '⚖️ Legal / Prosecutor', u: 'legal', p: 'Legal123!' },
              { label: '👨‍⚖️ Judge / Auditor', u: 'viewer', p: 'Viewer123!' },
            ].map((roleItem) => (
              <button
                key={roleItem.u}
                type="button"
                className="flex items-center justify-between rounded-lg bg-navy-800/80 px-2.5 py-1.5 text-left text-xs font-medium text-navy-200 transition hover:bg-accent-600 hover:text-white"
                onClick={() => {
                  setForm({ username: roleItem.u, password: roleItem.p });
                }}
              >
                <span>{roleItem.label}</span>
                <span className="text-[10px] opacity-70">Auto-fill</span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
