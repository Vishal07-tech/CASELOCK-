import { Link } from 'react-router-dom';
import { FolderLock, User } from 'lucide-react';
import StatusBadge from './StatusBadge';
import { CASE_STATUS_META, CASE_PRIORITY_META, CASE_TYPE_LABELS } from '../utils/constants';
import { formatDate } from '../utils/formatters';

export default function CaseCard({ caseItem }) {
  const statusMeta = CASE_STATUS_META[caseItem.status] || {};
  const priorityMeta = CASE_PRIORITY_META[caseItem.priority] || {};

  return (
    <Link
      to={`/cases/${caseItem.id}`}
      className="card block p-4 transition-shadow hover:shadow-md animate-fade-in"
    >
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2 text-xs font-medium text-slate-400">
          <FolderLock size={14} />
          {caseItem.caseNumber}
        </div>
        <StatusBadge label={statusMeta.label} tone={statusMeta.tone} />
      </div>
      <h3 className="mt-2 line-clamp-2 font-semibold text-slate-900">{caseItem.title}</h3>
      <div className="mt-3 flex items-center justify-between text-xs text-slate-500">
        <span className="inline-flex items-center gap-1">
          <User size={13} /> {caseItem.investigatorName || 'Unassigned'}
        </span>
        <StatusBadge label={priorityMeta.label} tone={priorityMeta.tone} />
      </div>
      <div className="mt-3 flex items-center justify-between border-t border-slate-100 pt-3 text-xs text-slate-400">
        <span>{CASE_TYPE_LABELS[caseItem.caseType]}</span>
        <span>{caseItem.evidenceCount ?? 0} evidence item(s)</span>
        <span>{formatDate(caseItem.updatedAt)}</span>
      </div>
    </Link>
  );
}
