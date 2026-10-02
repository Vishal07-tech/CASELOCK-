import clsx from 'clsx';

const TONE_CLASSES = {
  blue: 'bg-blue-50 text-blue-600',
  green: 'bg-emerald-50 text-emerald-600',
  amber: 'bg-amber-50 text-amber-600',
  red: 'bg-red-50 text-red-600',
  navy: 'bg-navy-50 text-navy-700',
};

export default function StatCard({ label, value, icon: Icon, tone = 'navy', hint }) {
  return (
    <div className="card flex items-center gap-4 p-4 animate-fade-in">
      <div className={clsx('flex h-11 w-11 shrink-0 items-center justify-center rounded-lg', TONE_CLASSES[tone])}>
        {Icon && <Icon size={20} />}
      </div>
      <div className="min-w-0">
        <p className="truncate text-xs font-medium uppercase tracking-wide text-slate-500">{label}</p>
        <p className="text-2xl font-semibold text-slate-900">{value}</p>
        {hint && <p className="mt-0.5 text-xs text-slate-400">{hint}</p>}
      </div>
    </div>
  );
}
