import { Loader2 } from 'lucide-react';
import clsx from 'clsx';

export default function LoadingSpinner({ label = 'Loading...', size = 20, fullPage = false, className }) {
  const content = (
    <div className={clsx('flex items-center justify-center gap-2 text-slate-500', className)}>
      <Loader2 size={size} className="animate-spin" />
      {label && <span className="text-sm">{label}</span>}
    </div>
  );

  if (fullPage) {
    return <div className="flex min-h-[60vh] w-full items-center justify-center">{content}</div>;
  }
  return <div className="py-10">{content}</div>;
}
