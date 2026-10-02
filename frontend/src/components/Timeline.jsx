import { ArrowRightLeft, CheckCircle2, Download, Eye, Lock, ShieldCheck, Unlock, Upload, Archive } from 'lucide-react';
import { CUSTODY_ACTION_LABELS } from '../utils/constants';
import { formatDateTime, shortHash } from '../utils/formatters';
import EmptyState from './EmptyState';

const ACTION_ICON = {
  COLLECTED: Upload,
  UPLOADED: Upload,
  ACCESSED: Eye,
  TRANSFERRED: ArrowRightLeft,
  ANALYZED: ShieldCheck,
  VERIFIED: CheckCircle2,
  DOWNLOADED: Download,
  RETURNED: ArrowRightLeft,
  SEALED: Lock,
  REOPENED: Unlock,
  ARCHIVED: Archive,
};

/**
 * Visualizes a chain-of-custody event list as a vertical timeline, exactly
 * matching the "Evidence Collected -> Uploaded -> Transferred -> Analyzed ->
 * ... -> Archived" narrative the spec calls for.
 */
export default function Timeline({ events = [] }) {
  if (!events.length) {
    return <EmptyState title="No custody events recorded yet" description="Chain-of-custody events will appear here as this evidence is handled." />;
  }

  return (
    <ol className="relative border-l border-slate-200 pl-6">
      {events.map((event, idx) => {
        const Icon = ACTION_ICON[event.action] || ShieldCheck;
        return (
          <li key={event.id ?? idx} className="mb-6 last:mb-0 animate-fade-in">
            <span className="absolute -left-[13px] flex h-6 w-6 items-center justify-center rounded-full bg-accent-600 text-white ring-4 ring-white">
              <Icon size={13} />
            </span>
            <div className="card p-3">
              <div className="flex flex-wrap items-center justify-between gap-1">
                <p className="font-medium text-slate-900">{CUSTODY_ACTION_LABELS[event.action] || event.action}</p>
                <time className="text-xs text-slate-400">{formatDateTime(event.eventTimestamp)}</time>
              </div>
              {(event.fromUserName || event.toUserName) && (
                <p className="mt-1 text-sm text-slate-600">
                  {event.fromUserName ? `${event.fromUserName} -> ` : ''}
                  {event.toUserName || 'Unassigned'}
                </p>
              )}
              {event.reason && <p className="mt-1 text-xs text-slate-500">Reason: {event.reason}</p>}
              {event.location && <p className="text-xs text-slate-500">Location: {event.location}</p>}
              {event.remarks && <p className="mt-1 text-xs italic text-slate-400">"{event.remarks}"</p>}
              {event.hashAtTransfer && (
                <p className="mt-1 font-mono text-[11px] text-slate-400">Hash: {shortHash(event.hashAtTransfer, 24)}</p>
              )}
            </div>
          </li>
        );
      })}
    </ol>
  );
}
