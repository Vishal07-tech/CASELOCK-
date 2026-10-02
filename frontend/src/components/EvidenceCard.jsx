import { Link } from 'react-router-dom';
import { FileText } from 'lucide-react';
import StatusBadge from './StatusBadge';
import { EVIDENCE_STATUS_META, INTEGRITY_STATUS_META, EVIDENCE_CATEGORY_LABELS } from '../utils/constants';
import { formatBytes, formatDate } from '../utils/formatters';

export default function EvidenceCard({ evidence }) {
  const statusMeta = EVIDENCE_STATUS_META[evidence.status] || {};
  const integrityMeta = INTEGRITY_STATUS_META[evidence.integrityStatus] || {};

  return (
    <Link
      to={`/evidence/${evidence.id}`}
      className="card block p-4 transition-shadow hover:shadow-md animate-fade-in"
    >
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2 text-xs font-medium text-slate-400">
          <FileText size={14} />
          {evidence.evidenceNumber}
        </div>
        <StatusBadge label={integrityMeta.label} tone={integrityMeta.tone} />
      </div>
      <h3 className="mt-2 line-clamp-1 font-semibold text-slate-900">{evidence.fileName}</h3>
      <p className="text-xs text-slate-400">{evidence.caseNumber}</p>
      <div className="mt-3 flex items-center justify-between text-xs text-slate-500">
        <span>{EVIDENCE_CATEGORY_LABELS[evidence.evidenceCategory]}</span>
        <StatusBadge label={statusMeta.label} tone={statusMeta.tone} />
      </div>
      <div className="mt-3 flex items-center justify-between border-t border-slate-100 pt-3 text-xs text-slate-400">
        <span>{formatBytes(evidence.fileSizeBytes)}</span>
        <span>{evidence.currentCustodianName || 'Unassigned'}</span>
        <span>{formatDate(evidence.uploadDate)}</span>
      </div>
    </Link>
  );
}
