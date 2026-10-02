import { ChevronRight } from 'lucide-react';
import { Link } from 'react-router-dom';

/**
 * items: [{ label, to }] - the last item is rendered as plain text (current page).
 * Keeping this consistent everywhere is what makes "Evidence -> Case ->
 * Dashboard" style back-navigation predictable across the app.
 */
export default function Breadcrumb({ items = [] }) {
  if (!items.length) return null;
  return (
    <nav className="mb-1 flex items-center gap-1 text-xs text-slate-500" aria-label="Breadcrumb">
      {items.map((item, idx) => {
        const isLast = idx === items.length - 1;
        return (
          <span key={idx} className="flex items-center gap-1">
            {idx > 0 && <ChevronRight size={12} className="text-slate-300" />}
            {isLast || !item.to ? (
              <span className="font-medium text-slate-600">{item.label}</span>
            ) : (
              <Link to={item.to} className="hover:text-accent-600 hover:underline">
                {item.label}
              </Link>
            )}
          </span>
        );
      })}
    </nav>
  );
}
