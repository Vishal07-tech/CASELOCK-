import clsx from 'clsx';
import { AlertTriangle, CheckCircle2, Circle, Clock, Lock, ShieldAlert, XCircle } from 'lucide-react';

const TONE_CLASSES = {
  blue: 'bg-blue-50 text-blue-700 ring-blue-600/20',
  green: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  amber: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  red: 'bg-red-50 text-red-700 ring-red-600/20',
  purple: 'bg-purple-50 text-purple-700 ring-purple-600/20',
  slate: 'bg-slate-100 text-slate-700 ring-slate-500/20',
  gray: 'bg-gray-100 text-gray-600 ring-gray-500/20',
  navy: 'bg-navy-100 text-navy-800 ring-navy-500/20',
};

const TONE_ICON = {
  green: CheckCircle2,
  red: XCircle,
  amber: AlertTriangle,
  navy: Lock,
  purple: ShieldAlert,
  blue: Clock,
};

/**
 * A status indicator that never relies on color alone: it always pairs an
 * icon with the text label so it stays meaningful for color-blind users and
 * in grayscale printouts (see accessibility requirements).
 */
export default function StatusBadge({ label, tone = 'slate', icon: IconOverride }) {
  const Icon = IconOverride || TONE_ICON[tone] || Circle;
  return (
    <span
      className={clsx(
        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ring-1 ring-inset',
        TONE_CLASSES[tone] || TONE_CLASSES.slate
      )}
    >
      <Icon size={13} strokeWidth={2.5} />
      {label}
    </span>
  );
}
