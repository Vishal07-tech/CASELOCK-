import { useState } from 'react';
import PageHeader from '../components/PageHeader';
import { notify } from '../components/Toast';
import { useAuth } from '../context/AuthContext';

export default function Settings() {
  const { user } = useAuth();
  const [emailAlerts, setEmailAlerts] = useState(true);
  const [transferAlerts, setTransferAlerts] = useState(true);
  const [integrityAlerts, setIntegrityAlerts] = useState(true);

  const handleSave = (e) => {
    e.preventDefault();
    // Notification preferences are a client-side convenience in this
    // build; wire this up to a PATCH /api/users/{id}/preferences endpoint
    // if/when persisted per-user settings are needed.
    notify.success('Preferences saved.');
  };

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader title="Settings" description="Manage your notification preferences and account security." />

      <form onSubmit={handleSave} className="card space-y-4 p-6">
        <h3 className="text-sm font-semibold text-slate-700">Notification Preferences</h3>
        <Toggle label="Email me about case assignments" checked={emailAlerts} onChange={setEmailAlerts} />
        <Toggle label="Notify me when evidence is transferred to me" checked={transferAlerts} onChange={setTransferAlerts} />
        <Toggle label="Notify me on integrity verification failures" checked={integrityAlerts} onChange={setIntegrityAlerts} />

        <div className="flex justify-end pt-2">
          <button type="submit" className="btn-accent">Save Preferences</button>
        </div>
      </form>

      <div className="card mt-4 p-6">
        <h3 className="mb-2 text-sm font-semibold text-slate-700">Account Security</h3>
        <p className="text-sm text-slate-500">
          To reset your password, contact your CaseLock administrator ({user?.role === 'ADMIN' ? 'or use User Management' : 'via the User Management team'}).
        </p>
      </div>
    </div>
  );
}

function Toggle({ label, checked, onChange }) {
  return (
    <label className="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-3 py-2.5 text-sm">
      {label}
      <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} className="h-4 w-4 accent-accent-600" />
    </label>
  );
}
