import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Download, Lock, ShieldCheck, Unlock, ArrowRightLeft, Loader2 } from 'lucide-react';
import evidenceService from '../services/evidenceService';
import userService from '../services/userService';
import PageHeader from '../components/PageHeader';
import Breadcrumb from '../components/Breadcrumb';
import StatusBadge from '../components/StatusBadge';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorState from '../components/ErrorState';
import Timeline from '../components/Timeline';
import Modal from '../components/Modal';
import ConfirmationDialog from '../components/ConfirmationDialog';
import { useAuth } from '../context/AuthContext';
import { apiErrorMessage } from '../services/api';
import { notify } from '../components/Toast';
import { formatBytes, formatDateTime } from '../utils/formatters';
import {
  EVIDENCE_STATUS_META, EVIDENCE_CATEGORY_LABELS, INTEGRITY_STATUS_META,
  CAN_VERIFY_EVIDENCE, CAN_TRANSFER_EVIDENCE, CAN_DOWNLOAD_EVIDENCE,
} from '../utils/constants';

export default function EvidenceDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasRole } = useAuth();

  const [evidence, setEvidence] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [custodyEvents, setCustodyEvents] = useState([]);
  const [verifying, setVerifying] = useState(false);
  const [transferOpen, setTransferOpen] = useState(false);
  const [sealDialogOpen, setSealDialogOpen] = useState(false);
  const [verifyResultOpen, setVerifyResultOpen] = useState(false);
  const [verifyResult, setVerifyResult] = useState(null);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [data, timeline] = await Promise.all([
        evidenceService.get(id),
        evidenceService.chainOfCustody(id),
      ]);
      setEvidence(data);
      setCustodyEvents(timeline);
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not load this evidence item.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [id]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleVerify = async () => {
    setVerifying(true);
    try {
      const result = await evidenceService.verifyIntegrity(id);
      setVerifyResult(result);
      setVerifyResultOpen(true);
      await load();
    } catch (err) {
      notify.error(apiErrorMessage(err, 'Could not verify evidence integrity.'));
    } finally {
      setVerifying(false);
    }
  };

  const handleDownload = async () => {
    try {
      await evidenceService.download(id, evidence.fileName);
    } catch (err) {
      notify.error(apiErrorMessage(err, 'Could not download the evidence file.'));
    }
  };

  const handleSealToggle = async () => {
    try {
      if (evidence.sealed) {
        await evidenceService.reopen(id);
        notify.success('Evidence reopened.');
      } else {
        await evidenceService.seal(id);
        notify.success('Evidence sealed.');
      }
      setSealDialogOpen(false);
      load();
    } catch (err) {
      notify.error(apiErrorMessage(err, 'Could not update evidence seal status.'));
    }
  };

  if (loading) return <LoadingSpinner fullPage label="Loading evidence..." />;
  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!evidence) return null;

  const statusMeta = EVIDENCE_STATUS_META[evidence.status] || {};
  const integrityMeta = INTEGRITY_STATUS_META[evidence.integrityStatus] || {};

  return (
    <div>
      <PageHeader
        title={evidence.fileName}
        breadcrumb={
          <Breadcrumb
            items={[
              { label: 'Evidence', to: '/evidence' },
              { label: 'Case ' + (evidence.caseNumber || ''), to: `/cases/${evidence.caseId}` },
              { label: evidence.evidenceNumber },
            ]}
          />
        }
        actions={
          <>
            {hasRole(...CAN_DOWNLOAD_EVIDENCE) && (
              <button className="btn-secondary" onClick={handleDownload}>
                <Download size={15} /> Download
              </button>
            )}
            {hasRole(...CAN_VERIFY_EVIDENCE) && (
              <button className="btn-secondary" onClick={handleVerify} disabled={verifying}>
                {verifying ? <Loader2 size={15} className="animate-spin" /> : <ShieldCheck size={15} />}
                {verifying ? 'Verifying...' : 'Verify Integrity'}
              </button>
            )}
            {hasRole(...CAN_TRANSFER_EVIDENCE) && !evidence.sealed && (
              <button className="btn-secondary" onClick={() => setTransferOpen(true)}>
                <ArrowRightLeft size={15} /> Transfer
              </button>
            )}
            {hasRole(...CAN_TRANSFER_EVIDENCE) && (
              <button className="btn-secondary" onClick={() => setSealDialogOpen(true)}>
                {evidence.sealed ? <Unlock size={15} /> : <Lock size={15} />}
                {evidence.sealed ? 'Reopen' : 'Seal'}
              </button>
            )}
          </>
        }
      />

      <div className="mb-4 flex flex-wrap items-center gap-2">
        <StatusBadge label={statusMeta.label} tone={statusMeta.tone} />
        <StatusBadge label={integrityMeta.label} tone={integrityMeta.tone} />
        {evidence.sealed && <StatusBadge label="Sealed" tone="navy" icon={Lock} />}
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <section className="card p-5">
            <h3 className="mb-3 text-sm font-semibold text-slate-700">Evidence Information</h3>
            <div className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
              <DetailRow label="Evidence Number" value={evidence.evidenceNumber} />
              <DetailRow label="Category" value={EVIDENCE_CATEGORY_LABELS[evidence.evidenceCategory]} />
              <DetailRow label="Description" value={evidence.description || '-'} wide />
              <DetailRow label="Uploaded By" value={evidence.uploadedBy?.fullName} />
              <DetailRow label="Upload Date" value={formatDateTime(evidence.uploadDate)} />
              <DetailRow label="Current Custodian" value={evidence.currentCustodian?.fullName} />
            </div>
          </section>

          <section className="card p-5">
            <h3 className="mb-3 text-sm font-semibold text-slate-700">File Information</h3>
            <div className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
              <DetailRow label="File Name" value={evidence.fileName} />
              <DetailRow label="File Type" value={evidence.fileType || 'unknown'} />
              <DetailRow label="File Size" value={formatBytes(evidence.fileSizeBytes)} />
            </div>
          </section>

          <section className="card p-5">
            <h3 className="mb-3 text-sm font-semibold text-slate-700">Hash Information &amp; Integrity Status</h3>
            <div className="space-y-2 text-sm">
              <DetailRow label="Original SHA-256 (immutable)" value={<code className="text-xs">{evidence.originalSha256}</code>} wide />
              <DetailRow label="Current SHA-256" value={<code className="text-xs">{evidence.currentSha256 || '-'}</code>} wide />
              <DetailRow label="Last Verified" value={formatDateTime(evidence.lastVerifiedAt)} />
              <div className="flex items-center justify-between">
                <span className="text-slate-400">Integrity Status</span>
                <StatusBadge label={integrityMeta.label} tone={integrityMeta.tone} />
              </div>
            </div>
            <p className="mt-3 rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-500">
              A matching hash confirms this file's digital content is unchanged since registration. It does not, on
              its own, prove the evidence is authentic - refer to the chain of custody below for that context.
            </p>
          </section>

          <section className="card p-5">
            <h3 className="mb-3 text-sm font-semibold text-slate-700">Chain of Custody</h3>
            <Timeline events={custodyEvents} />
          </section>
        </div>

        <div className="space-y-4">
          <section className="card p-5">
            <h3 className="mb-3 text-sm font-semibold text-slate-700">Access History</h3>
            {custodyEvents.filter((e) => e.action === 'ACCESSED' || e.action === 'DOWNLOADED').length === 0 ? (
              <p className="text-sm text-slate-400">No access events recorded yet.</p>
            ) : (
              <ul className="space-y-2 text-xs">
                {custodyEvents
                  .filter((e) => e.action === 'ACCESSED' || e.action === 'DOWNLOADED')
                  .map((e) => (
                    <li key={e.id} className="flex justify-between border-b border-slate-100 pb-1.5 last:border-0">
                      <span>{e.action === 'ACCESSED' ? 'Viewed' : 'Downloaded'} by {e.toUserName || 'unknown'}</span>
                      <span className="text-slate-400">{formatDateTime(e.eventTimestamp)}</span>
                    </li>
                  ))}
              </ul>
            )}
          </section>
        </div>
      </div>

      <TransferModal
        open={transferOpen}
        onClose={() => setTransferOpen(false)}
        evidenceId={id}
        onTransferred={() => { setTransferOpen(false); load(); }}
      />

      <ConfirmationDialog
        open={sealDialogOpen}
        title={evidence.sealed ? 'Reopen this evidence?' : 'Seal this evidence?'}
        message={
          evidence.sealed
            ? 'Reopening will allow this evidence to be transferred again.'
            : 'Sealing this evidence will prevent further transfers until it is reopened.'
        }
        confirmLabel={evidence.sealed ? 'Reopen' : 'Seal'}
        onConfirm={handleSealToggle}
        onCancel={() => setSealDialogOpen(false)}
      />

      <Modal
        open={verifyResultOpen}
        onClose={() => setVerifyResultOpen(false)}
        title="Integrity Verification Result"
        footer={<button className="btn-accent" onClick={() => setVerifyResultOpen(false)}>Close</button>}
      >
        {verifyResult && (
          <div className="space-y-3 text-sm">
            <div className="flex justify-center">
              <StatusBadge
                label={verifyResult.match ? 'INTEGRITY VERIFIED' : 'INTEGRITY COMPROMISED'}
                tone={verifyResult.match ? 'green' : 'red'}
              />
            </div>
            <p className="text-center text-slate-600">{verifyResult.message}</p>
            <DetailRow label="Original Hash" value={<code className="text-xs">{verifyResult.originalSha256}</code>} wide />
            <DetailRow label="Current Hash" value={<code className="text-xs">{verifyResult.currentSha256}</code>} wide />
          </div>
        )}
      </Modal>
    </div>
  );
}

function DetailRow({ label, value, wide }) {
  return (
    <div className={wide ? 'sm:col-span-2' : ''}>
      <p className="text-xs text-slate-400">{label}</p>
      <p className="break-words font-medium text-slate-700">{value ?? '-'}</p>
    </div>
  );
}

function TransferModal({ open, onClose, evidenceId, onTransferred }) {
  const [users, setUsers] = useState([]);
  const [toUserId, setToUserId] = useState('');
  const [reason, setReason] = useState('');
  const [location, setLocation] = useState('');
  const [remarks, setRemarks] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (open) {
      userService.directory().then((d) => setUsers(d.content)).catch(() => {});
      setToUserId(''); setReason(''); setLocation(''); setRemarks(''); setError('');
    }
  }, [open]);

  const handleSubmit = async () => {
    if (!toUserId || !reason) {
      setError('Please select a recipient and provide a reason for the transfer.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      await evidenceService.transfer(evidenceId, { toUserId: Number(toUserId), reason, location, remarks });
      notify.success('Evidence transferred successfully.');
      onTransferred();
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not transfer this evidence.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Transfer Evidence"
      footer={
        <>
          <button className="btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn-accent" onClick={handleSubmit} disabled={submitting}>
            {submitting ? 'Transferring...' : 'Transfer'}
          </button>
        </>
      }
    >
      {error && <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}
      <div className="space-y-3">
        <div>
          <label className="label">Transfer To <span className="text-red-500">*</span></label>
          <select className="input" value={toUserId} onChange={(e) => setToUserId(e.target.value)}>
            <option value="">Select recipient...</option>
            {users.map((u) => <option key={u.id} value={u.id}>{u.fullName}</option>)}
          </select>
        </div>
        <div>
          <label className="label">Reason <span className="text-red-500">*</span></label>
          <input className="input" value={reason} onChange={(e) => setReason(e.target.value)} placeholder="e.g. Forensic analysis" />
        </div>
        <div>
          <label className="label">Location</label>
          <input className="input" value={location} onChange={(e) => setLocation(e.target.value)} />
        </div>
        <div>
          <label className="label">Remarks</label>
          <textarea className="input" rows={2} value={remarks} onChange={(e) => setRemarks(e.target.value)} />
        </div>
      </div>
    </Modal>
  );
}
