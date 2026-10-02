import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import caseService from '../services/caseService';
import userService from '../services/userService';
import PageHeader from '../components/PageHeader';
import Breadcrumb from '../components/Breadcrumb';
import { notify } from '../components/Toast';
import { apiErrorMessage } from '../services/api';
import { CASE_TYPES, CASE_TYPE_LABELS, CASE_PRIORITIES, CASE_PRIORITY_META } from '../utils/constants';
import { Loader2 } from 'lucide-react';

const INITIAL = {
  title: '', description: '', caseType: 'CRIMINAL', priority: 'MEDIUM',
  investigatorId: '', assignedTeamIds: [], location: '', caseStartDate: '',
};

export default function CaseNew() {
  const navigate = useNavigate();
  const [form, setForm] = useState(INITIAL);
  const [users, setUsers] = useState([]);
  const [errors, setErrors] = useState({});
  const [submitError, setSubmitError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    userService.directory().then((data) => setUsers(data.content)).catch(() => {});
  }, []);

  const update = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const toggleTeamMember = (id) => {
    setForm((f) => ({
      ...f,
      assignedTeamIds: f.assignedTeamIds.includes(id)
        ? f.assignedTeamIds.filter((x) => x !== id)
        : [...f.assignedTeamIds, id],
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitError('');
    setErrors({});
    setSubmitting(true);
    try {
      const payload = { ...form, investigatorId: Number(form.investigatorId), caseStartDate: form.caseStartDate || undefined };
      const created = await caseService.create(payload);
      notify.success('Case created successfully.');
      navigate(`/cases/${created.id}`);
    } catch (err) {
      const fieldErrors = err?.response?.data?.data;
      if (fieldErrors && typeof fieldErrors === 'object') setErrors(fieldErrors);
      setSubmitError(apiErrorMessage(err, 'Could not create the case. Please check the form and try again.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader
        title="Create New Case"
        breadcrumb={<Breadcrumb items={[{ label: 'Cases', to: '/cases' }, { label: 'New Case' }]} />}
      />

      <form onSubmit={handleSubmit} className="card space-y-4 p-6">
        {submitError && <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{submitError}</div>}

        <div>
          <label className="label">Case Title <span className="text-red-500">*</span></label>
          <input required className="input" value={form.title} onChange={update('title')} placeholder="e.g. Downtown Data Breach Investigation" />
          {errors.title && <p className="mt-1 text-xs text-red-600">{errors.title}</p>}
        </div>

        <div>
          <label className="label">Description</label>
          <textarea rows={4} className="input" value={form.description} onChange={update('description')} />
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="label">Case Type <span className="text-red-500">*</span></label>
            <select required className="input" value={form.caseType} onChange={update('caseType')}>
              {CASE_TYPES.map((t) => <option key={t} value={t}>{CASE_TYPE_LABELS[t]}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Priority</label>
            <select className="input" value={form.priority} onChange={update('priority')}>
              {CASE_PRIORITIES.map((p) => <option key={p} value={p}>{CASE_PRIORITY_META[p].label}</option>)}
            </select>
          </div>
        </div>

        <div>
          <label className="label">Investigator <span className="text-red-500">*</span></label>
          <select required className="input" value={form.investigatorId} onChange={update('investigatorId')}>
            <option value="">Select an investigator...</option>
            {users.map((u) => <option key={u.id} value={u.id}>{u.fullName}</option>)}
          </select>
          {errors.investigatorId && <p className="mt-1 text-xs text-red-600">{errors.investigatorId}</p>}
        </div>

        <div>
          <label className="label">Assigned Team</label>
          <div className="max-h-40 space-y-1 overflow-y-auto rounded-lg border border-slate-200 p-2">
            {users.map((u) => (
              <label key={u.id} className="flex items-center gap-2 rounded px-1.5 py-1 text-sm hover:bg-slate-50">
                <input
                  type="checkbox"
                  checked={form.assignedTeamIds.includes(u.id)}
                  onChange={() => toggleTeamMember(u.id)}
                />
                {u.fullName}
              </label>
            ))}
          </div>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="label">Location</label>
            <input className="input" value={form.location} onChange={update('location')} />
          </div>
          <div>
            <label className="label">Case Start Date</label>
            <input type="date" className="input" value={form.caseStartDate} onChange={update('caseStartDate')} />
          </div>
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <button type="button" className="btn-secondary" onClick={() => navigate('/cases')}>Cancel</button>
          <button type="submit" className="btn-accent" disabled={submitting}>
            {submitting && <Loader2 size={16} className="animate-spin" />}
            {submitting ? 'Creating...' : 'Create Case'}
          </button>
        </div>
      </form>
    </div>
  );
}
