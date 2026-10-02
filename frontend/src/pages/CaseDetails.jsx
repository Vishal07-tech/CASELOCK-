import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Edit, FileBarChart, Plus, Upload } from 'lucide-react';
import caseService from '../services/caseService';
import evidenceService from '../services/evidenceService';
import custodyService from '../services/custodyService';
import auditService from '../services/auditService';
import PageHeader from '../components/PageHeader';
import Breadcrumb from '../components/Breadcrumb';
import StatusBadge from '../components/StatusBadge';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import Timeline from '../components/Timeline';
import EvidenceCard from '../components/EvidenceCard';
import Modal from '../components/Modal';
import { useAuth } from '../context/AuthContext';
import { apiErrorMessage } from '../services/api';
import { formatDate, formatDateTime } from '../utils/formatters';
import {
  CASE_STATUS_META, CASE_PRIORITY_META, CASE_TYPE_LABELS, CASE_STATUSES,
  CAN_EDIT_CASE, CAN_UPLOAD_EVIDENCE, CAN_VIEW_AUDIT_LOGS,
} from '../utils/constants';
import EvidenceUploadForm from '../components/EvidenceUploadForm';

const TABS = ['Overview', 'Evidence', 'Chain of Custody', 'Audit History', 'Team'];

export default function CaseDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, hasRole } = useAuth();

  const [caseItem, setCaseItem] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tab, setTab] = useState('Overview');

  const [evidenceList, setEvidenceList] = useState([]);
  const [custodyEvents, setCustodyEvents] = useState([]);
  const [auditEntries, setAuditEntries] = useState([]);
  const [statusModalOpen, setStatusModalOpen] = useState(false);
  const [uploadModalOpen, setUploadModalOpen] = useState(false);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await caseService.get(id);
      setCaseItem(data);
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not load this case.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [id]);

  useEffect(() => {
    if (!caseItem) return;
    if (tab === 'Evidence') {
      evidenceService.list({ caseId: id, size: 50 }).then((d) => setEvidenceList(d.content)).catch(() => {});
    } else if (tab === 'Chain of Custody') {
      custodyService.forCase(id, { size: 50 }).then((d) => setCustodyEvents(d.content)).catch(() => {});
    } else if (tab === 'Audit History' && hasRole(...CAN_VIEW_AUDIT_LOGS)) {
      auditService.search({ entityType: 'Case', entityId: id, size: 50 }).then((d) => setAuditEntries(d.content)).catch(() => {});
    }
  }, [tab, caseItem, id]); // eslint-disable-line react-hooks/exhaustive-deps

  if (loading) return <LoadingSpinner fullPage label="Loading case..." />;
  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!caseItem) return null;

  const statusMeta = CASE_STATUS_META[caseItem.status] || {};
  const priorityMeta = CASE_PRIORITY_META[caseItem.priority] || {};
  const canEdit = hasRole(...CAN_EDIT_CASE);
  const canUpload = hasRole(...CAN_UPLOAD_EVIDENCE);

  return (
    <div>
      <PageHeader
        title={caseItem.title}
        breadcrumb={<Breadcrumb items={[{ label: 'Cases', to: '/cases' }, { label: caseItem.caseNumber }]} />}
        actions={
          <>
            {canUpload && (
              <button className="btn-secondary" onClick={() => setUploadModalOpen(true)}>
                <Upload size={15} /> Upload Evidence
              </button>
            )}
            {canEdit && (
              <button className="btn-secondary" onClick={() => setStatusModalOpen(true)}>
                Change Status
              </button>
            )}
            {canEdit && (
              <button className="btn-accent" onClick={() => navigate(`/cases/${id}/edit`)}>
                <Edit size={15} /> Edit
              </button>
            )}
            <Link to={`/reports?caseId=${id}`} className="btn-secondary">
              <FileBarChart size={15} /> Report
            </Link>
          </>
        }
      />

      <div className="mb-4 flex flex-wrap items-center gap-2">
        <StatusBadge label={statusMeta.label} tone={statusMeta.tone} />
        <StatusBadge label={priorityMeta.label} tone={priorityMeta.tone} />
        <span className="text-sm text-slate-400">{CASE_TYPE_LABELS[caseItem.caseType]}</span>
      </div>

      <div className="mb-5 border-b border-slate-200">
        <nav className="-mb-px flex gap-5 overflow-x-auto">
          {TABS.map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`whitespace-nowrap border-b-2 px-1 py-2.5 text-sm font-medium transition-colors ${
                tab === t ? 'border-accent-600 text-accent-600' : 'border-transparent text-slate-500 hover:text-slate-700'
              }`}
            >
              {t}
            </button>
          ))}
        </nav>
      </div>

      {tab === 'Overview' && (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
          <div className="card space-y-3 p-5 lg:col-span-2">
            <h3 className="text-sm font-semibold text-slate-700">Case Description</h3>
            <p className="whitespace-pre-line text-sm text-slate-600">{caseItem.description || 'No description provided.'}</p>
          </div>
          <div className="card space-y-3 p-5 text-sm">
            <DetailRow label="Case Number" value={caseItem.caseNumber} />
            <DetailRow label="Investigator" value={caseItem.investigator?.fullName} />
            <DetailRow label="Location" value={caseItem.location} />
            <DetailRow label="Start Date" value={formatDate(caseItem.caseStartDate)} />
            <DetailRow label="Closing Date" value={formatDate(caseItem.caseClosingDate)} />
            <DetailRow label="Evidence Items" value={caseItem.evidenceCount} />
            <DetailRow label="Created" value={formatDateTime(caseItem.createdAt)} />
            <DetailRow label="Last Updated" value={formatDateTime(caseItem.updatedAt)} />
          </div>
        </div>
      )}

      {tab === 'Evidence' && (
        <div>
          {evidenceList.length === 0 ? (
            <EmptyState
              title="No evidence registered for this case"
              action={canUpload && (
                <button className="btn-accent" onClick={() => setUploadModalOpen(true)}>
                  <Plus size={15} /> Upload Evidence
                </button>
              )}
            />
          ) : (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {evidenceList.map((e) => <EvidenceCard key={e.id} evidence={e} />)}
            </div>
          )}
        </div>
      )}

      {tab === 'Chain of Custody' && <Timeline events={custodyEvents} />}

      {tab === 'Audit History' && (
        hasRole(...CAN_VIEW_AUDIT_LOGS) ? (
          auditEntries.length === 0 ? (
            <EmptyState title="No audit entries yet" />
          ) : (
            <ul className="space-y-2">
              {auditEntries.map((log) => (
                <li key={log.id} className="card flex items-start justify-between gap-3 p-3 text-sm">
                  <div>
                    <p className="text-slate-700">{log.description}</p>
                    <p className="text-xs text-slate-400">{log.username} &middot; {log.action}</p>
                  </div>
                  <span className="shrink-0 text-xs text-slate-400">{formatDateTime(log.eventTimestamp)}</span>
                </li>
              ))}
            </ul>
          )
        ) : (
          <EmptyState title="Administrator access required" description="Only administrators can view the full audit trail for this case." />
        )
      )}

      {tab === 'Team' && (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {caseItem.investigator && (
            <TeamMemberCard user={caseItem.investigator} roleLabel="Lead Investigator" />
          )}
          {caseItem.assignedTeam?.map((member) => (
            <TeamMemberCard key={member.id} user={member} roleLabel="Team Member" />
          ))}
        </div>
      )}

      <StatusChangeModal
        open={statusModalOpen}
        onClose={() => setStatusModalOpen(false)}
        caseItem={caseItem}
        onUpdated={(updated) => { setCaseItem(updated); setStatusModalOpen(false); }}
      />

      <Modal open={uploadModalOpen} onClose={() => setUploadModalOpen(false)} title="Upload Evidence" size="lg">
        <EvidenceUploadForm
          caseId={id}
          onSuccess={() => { setUploadModalOpen(false); setTab('Evidence'); evidenceService.list({ caseId: id, size: 50 }).then((d) => setEvidenceList(d.content)); load(); }}
        />
      </Modal>
    </div>
  );
}

function DetailRow({ label, value }) {
  return (
    <div className="flex items-center justify-between gap-3 border-b border-slate-100 pb-2 last:border-0 last:pb-0">
      <span className="text-slate-400">{label}</span>
      <span className="font-medium text-slate-700">{value ?? '-'}</span>
    </div>
  );
}

function TeamMemberCard({ user, roleLabel }) {
  return (
    <div className="card flex items-center gap-3 p-3">
      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-navy-900 text-xs font-semibold text-white">
        {user.fullName?.charAt(0)}
      </div>
      <div className="min-w-0">
        <p className="truncate text-sm font-medium text-slate-800">{user.fullName}</p>
        <p className="text-xs text-slate-400">{roleLabel}</p>
      </div>
    </div>
  );
}

function StatusChangeModal({ open, onClose, caseItem, onUpdated }) {
  const [status, setStatus] = useState(caseItem?.status);
  const [remarks, setRemarks] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => { setStatus(caseItem?.status); setRemarks(''); setError(''); }, [caseItem, open]);

  const handleSubmit = async () => {
    setSubmitting(true);
    setError('');
    try {
      const updated = await caseService.updateStatus(caseItem.id, status, remarks || undefined);
      onUpdated(updated);
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not update case status.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Change Case Status"
      footer={
        <>
          <button className="btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn-accent" onClick={handleSubmit} disabled={submitting}>
            {submitting ? 'Updating...' : 'Update Status'}
          </button>
        </>
      }
    >
      {error && <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}
      <label className="label">New Status</label>
      <select className="input mb-3" value={status} onChange={(e) => setStatus(e.target.value)}>
        {CASE_STATUSES.map((s) => <option key={s} value={s}>{CASE_STATUS_META[s].label}</option>)}
      </select>
      <label className="label">Remarks (optional)</label>
      <textarea className="input" rows={3} value={remarks} onChange={(e) => setRemarks(e.target.value)} />
    </Modal>
  );
}
