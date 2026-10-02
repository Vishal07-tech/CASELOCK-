import { SlidersHorizontal } from 'lucide-react';

/**
 * A simple horizontal filter bar: `filters` is [{ label, value, onChange,
 * options: [{value, label}] }]. Kept intentionally generic so every list
 * page (Cases, Evidence, Audit Logs, Users) can reuse it instead of
 * hand-rolling its own <select> row.
 */
export default function FilterPanel({ filters = [], onClear }) {
  const hasActiveFilters = filters.some((f) => f.value);

  return (
    <div className="flex flex-wrap items-center gap-2">
      <SlidersHorizontal size={15} className="text-slate-400" />
      {filters.map((filter) => (
        <select
          key={filter.label}
          value={filter.value || ''}
          onChange={(e) => filter.onChange(e.target.value || undefined)}
          className="input w-auto py-1.5 text-sm"
          aria-label={filter.label}
        >
          <option value="">{filter.label}</option>
          {filter.options.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      ))}
      {hasActiveFilters && onClear && (
        <button onClick={onClear} className="text-xs font-medium text-accent-600 hover:underline">
          Clear filters
        </button>
      )}
    </div>
  );
}
