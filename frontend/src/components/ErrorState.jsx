import { AlertOctagon, RefreshCw } from 'lucide-react';

export default function ErrorState({ message = 'Something went wrong while loading this page.', onRetry }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-14 text-center animate-fade-in">
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-red-50 text-red-500">
        <AlertOctagon size={22} />
      </div>
      <p className="max-w-sm text-sm text-slate-600">{message}</p>
      {onRetry && (
        <button onClick={onRetry} className="btn-secondary mt-1">
          <RefreshCw size={15} /> Try again
        </button>
      )}
    </div>
  );
}
