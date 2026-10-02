import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import caseService from '../services/caseService';
import userService from '../services/userService';
import PageHeader from '../components/PageHeader';
import Breadcrumb from '../components/Breadcrumb';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorState from '../components/ErrorState';
import { notify } from '../components/Toast';
import { apiErrorMessage } from '../services/api';
import { CASE_TYPES, CASE_TYPE_LABELS, CASE_PRIORITIES, CASE_PRIORITY_META } from '../utils/constants';

export default function CaseEdit() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [form, setForm] = useState(null);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [submitError, setSubmitError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    Promise.all([caseService.get(id), userService.directory()])
      .then(([caseItem, directory]) => {
        setForm({
          title: caseItem.title,
          description: caseItem.description || '',
          caseType: caseItem.caseType,
          priority: caseItem.priority,
          investigatorId: caseItem.investigator?.id || '',
          assignedTeamIds: caseItem.assignedTeam?.map((u) => u.id) || [],
          location: caseItem.location || '',
          caseStartDate: caseItem.caseStartDate || '',
          caseClosingDate: caseItem.caseClosingDate || '',
        });
        setUsers(directory.content);
      })
      .catch((err) => setError(apiErrorMessage(err, 'Could not load this case.')))
      .finally(() => setLoading(false));
  }, [id]);

  const update = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const toggleTeamMember = (userId) => {
    setForm((f) => ({
      ...f,
      assignedTeamIds: f.assignedTeamIds.includes(userId)
        ? f.assignedTeamIds.filter((x) => x !== userId)
        : [...f.assignedTeamIds, userId],
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitError('');
    setSubmitting(true);
    try {
      const payload = { ...form, investigatorId: form.investigatorId ? Number(form.investigatorId) : undefined };
      await caseService.update(id, payload);
      notify.success('Case updated successfully.');
      navigate(`/cases/${id}`);
    } catch (err) {
      setSubmitError(apiErrorMessage(err, 'Could not update the case. Please try again.'));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <LoadingSpinner fullPage label="Loading case..." />;
  if (error) return <ErrorState message={error} />;
  if (!form) return null;

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader
        title="Edit Case"
        breadcrumb={<Breadcrumb items={[{ label: 'Cases', to: '/cases' }, { label: 'Edit' }]} />}
      />

      <form onSubmit={handleSubmit} className="card space-y-4 p-6">
        {submitError && <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{submitError}</div>}

        <div>
          <label className="label">Case Title</label>
          <input required className="input" value={form.title} onChange={update('title')} />
        </div>

        <div>
          <label className="label">Description</label>
          <textarea rows={4} className="input" value={form.description} onChange={update('description')} />
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="label">Case Type</label>
            <select className="input" value={form.caseType} onChange={update('caseType')}>
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
          <label className="label">Investigator</label>
          <select className="input" value={form.investigatorId} onChange={update('investigatorId')}>
            {users.map((u) => <option key={u.id} value={u.id}>{u.fullName}</option>)}
          </select>
        </div>

        <div>
          <label className="label">Assigned Team</label>
          <div className="max-h-40 space-y-1 overflow-y-auto rounded-lg border border-slate-200 p-2">
            {users.map((u) => (
              <label key={u.id} className="flex items-center gap-2 rounded px-1.5 py-1 text-sm hover:bg-slate-50">
                <input type="checkbox" checked={form.assignedTeamIds.includes(u.id)} onChange={() => toggleTeamMember(u.id)} />
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

        <div>
          <label className="label">Case Closing Date</label>
          <input type="date" className="input" value={form.caseClosingDate} onChange={update('caseClosingDate')} />
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <button type="button" className="btn-secondary" onClick={() => navigate(`/cases/${id}`)}>Cancel</button>
          <button type="submit" className="btn-accent" disabled={submitting}>
            {submitting && <Loader2 size={16} className="animate-spin" />}
            {submitting ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </form>
    </div>
  );
}
