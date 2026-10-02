import { Link } from 'react-router-dom';
import { ShieldOff } from 'lucide-react';

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-slate-50 px-4 text-center">
      <ShieldOff className="text-slate-300" size={48} />
      <h1 className="text-2xl font-bold text-slate-800">Page not found</h1>
      <p className="text-sm text-slate-500">The page you're looking for doesn't exist or you don't have access to it.</p>
      <Link to="/dashboard" className="btn-accent mt-2">Back to Dashboard</Link>
    </div>
  );
}
