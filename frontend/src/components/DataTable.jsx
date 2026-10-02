import LoadingSpinner from './LoadingSpinner';
import EmptyState from './EmptyState';
import ErrorState from './ErrorState';
import Pagination from './Pagination';

/**
 * Generic, reusable table: `columns` is [{ key, header, render? }],
 * `pageData` is the PageResponse shape returned by the backend
 * ({ content, page, totalPages, totalElements }). Handles the
 * loading/empty/error states every list page needs (see spec #32) in one
 * place instead of duplicating them.
 */
export default function DataTable({
  columns,
  pageData,
  loading,
  error,
  onRetry,
  onRowClick,
  onPageChange,
  emptyTitle = 'No records found',
  emptyDescription,
}) {
  if (loading) return <LoadingSpinner label="Loading records..." />;
  if (error) return <ErrorState message={error} onRetry={onRetry} />;

  const rows = pageData?.content || [];

  if (!rows.length) {
    return <EmptyState title={emptyTitle} description={emptyDescription} />;
  }

  return (
    <div className="overflow-hidden rounded-xl border border-slate-200">
      <div className="overflow-x-auto">
        <table className="table-base w-full">
          <thead>
            <tr>
              {columns.map((col) => (
                <th key={col.key}>{col.header}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row, idx) => (
              <tr
                key={row.id ?? idx}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
                className={onRowClick ? 'cursor-pointer' : ''}
              >
                {columns.map((col) => (
                  <td key={col.key}>{col.render ? col.render(row) : row[col.key]}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {pageData && onPageChange && (
        <Pagination
          page={pageData.page}
          totalPages={pageData.totalPages}
          totalElements={pageData.totalElements}
          onPageChange={onPageChange}
        />
      )}
    </div>
  );
}
