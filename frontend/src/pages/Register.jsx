import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShieldCheck, Loader2 } from 'lucide-react';
import authService from '../services/authService';
import { apiErrorMessage } from '../services/api';
import { notify } from '../components/Toast';
import { ROLES, ROLE_LABELS } from '../utils/constants';

const INITIAL_FORM = {
  username: '', email: '', password: '', fullName: '', badgeNumber: '', department: '', phoneNumber: '', role: 'VIEWER',
};

export default function Register() {
  const navigate = useNavigate();
  const [form, setForm] = useState(INITIAL_FORM);
  const [errors, setErrors] = useState({});
  const [submitError, setSubmitError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const update = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitError('');
    setErrors({});
    setSubmitting(true);
    try {
      await authService.register(form);
      notify.success('Account created. You can now log in.');
      navigate('/login');
    } catch (err) {
      const fieldErrors = err?.response?.data?.data;
      if (fieldErrors && typeof fieldErrors === 'object') {
        setErrors(fieldErrors);
      }
      setSubmitError(apiErrorMessage(err, 'Registration failed. Please check the form and try again.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-navy-950 px-4 py-10">
      <div className="w-full max-w-lg animate-slide-up">
        <div className="mb-6 flex flex-col items-center text-center">
          <div className="mb-3 flex h-14 w-14 items-center justify-center rounded-2xl bg-accent-600">
            <ShieldCheck className="text-white" size={28} />
          </div>
          <h1 className="text-2xl font-bold text-white">Create your CaseLock account</h1>
          <p className="mt-1 text-sm text-navy-300">For authorized personnel only.</p>
        </div>

        <form onSubmit={handleSubmit} className="card space-y-4 p-6">
          {submitError && (
            <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
              {submitError}
            </div>
          )}

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Full name" required error={errors.fullName}>
              <input required className="input" value={form.fullName} onChange={update('fullName')} />
            </Field>
            <Field label="Username" required error={errors.username}>
              <input required className="input" value={form.username} onChange={update('username')} />
            </Field>
          </div>

          <Field label="Email address" required error={errors.email}>
            <input required type="email" className="input" value={form.email} onChange={update('email')} />
          </Field>

          <Field label="Password" required error={errors.password}>
            <input required type="password" minLength={8} className="input" value={form.password} onChange={update('password')} />
          </Field>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Badge / ID number">
              <input className="input" value={form.badgeNumber} onChange={update('badgeNumber')} />
            </Field>
            <Field label="Department">
              <input className="input" value={form.department} onChange={update('department')} />
            </Field>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Phone number">
              <input className="input" value={form.phoneNumber} onChange={update('phoneNumber')} />
            </Field>
            <Field label="Requested role" required>
              <select className="input" value={form.role} onChange={update('role')}>
                {ROLES.map((r) => (
                  <option key={r} value={r}>
                    {ROLE_LABELS[r]}
                  </option>
                ))}
              </select>
            </Field>
          </div>
          <p className="text-xs text-slate-400">
            An administrator may adjust your role after account creation based on your actual duties.
          </p>

          <button type="submit" className="btn-accent w-full" disabled={submitting}>
            {submitting ? <Loader2 size={16} className="animate-spin" /> : null}
            {submitting ? 'Creating account...' : 'Create account'}
          </button>

          <p className="text-center text-sm text-slate-500">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-accent-600 hover:underline">
              Sign in
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}

function Field({ label, required, error, children }) {
  return (
    <div>
      <label className="label">
        {label} {required && <span className="text-red-500">*</span>}
      </label>
      {children}
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  );
}
