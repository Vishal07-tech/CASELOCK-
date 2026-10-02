import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Plus, LayoutGrid, List } from 'lucide-react';
import caseService from '../services/caseService';
import PageHeader from '../components/PageHeader';
import SearchBar from '../components/SearchBar';
import FilterPanel from '../components/FilterPanel';
import DataTable from '../components/DataTable';
import CaseCard from '../components/CaseCard';
import StatusBadge from '../components/StatusBadge';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import useDebounce from '../hooks/useDebounce';
import { useAuth } from '../context/AuthContext';
import { apiErrorMessage } from '../services/api';
import { formatDate } from '../utils/formatters';
import { CASE_STATUSES, CASE_STATUS_META, CASE_PRIORITIES, CASE_PRIORITY_META, CAN_CREATE_CASE } from '../utils/constants';

export default function Cases() {
  const { hasRole } = useAuth();
  const navigate = useNavigate();

  const [keyword, setKeyword] = useState('');
  const debouncedKeyword = useDebounce(keyword);
  const [status, setStatus] = useState();
  const [priority, setPriority] = useState();
  const [page, setPage] = useState(0);
  const [view, setView] = useState('grid');

  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await caseService.list({ keyword: debouncedKeyword || undefined, status, priority, page, size: 12 });
      setPageData(data);
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not load cases.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedKeyword, status, priority, page]);

  useEffect(() => setPage(0), [debouncedKeyword, status, priority]);

  const columns = [
    { key: 'caseNumber', header: 'Case #' },
    { key: 'title', header: 'Title' },
    { key: 'caseType', header: 'Type' },
    {
      key: 'priority', header: 'Priority',
      render: (row) => <StatusBadge label={CASE_PRIORITY_META[row.priority]?.label} tone={CASE_PRIORITY_META[row.priority]?.tone} />,
    },
    {
      key: 'status', header: 'Status',
      render: (row) => <StatusBadge label={CASE_STATUS_META[row.status]?.label} tone={CASE_STATUS_META[row.status]?.tone} />,
    },
    { key: 'investigatorName', header: 'Investigator' },
    { key: 'evidenceCount', header: 'Evidence' },
    { key: 'updatedAt', header: 'Updated', render: (row) => formatDate(row.updatedAt) },
  ];

  const canCreate = hasRole(...CAN_CREATE_CASE);

  return (
    <div>
      <PageHeader
        title="Cases"
        description="Every case you're authorized to view, with its current status and evidence count."
        actions={
          <>
            <div className="flex overflow-hidden rounded-lg border border-slate-300">
              <button
                className={`px-2.5 py-2 ${view === 'grid' ? 'bg-slate-100' : 'bg-white'}`}
                onClick={() => setView('grid')}
                aria-label="Grid view"
              >
                <LayoutGrid size={15} />
              </button>
              <button
                className={`px-2.5 py-2 ${view === 'list' ? 'bg-slate-100' : 'bg-white'}`}
                onClick={() => setView('list')}
                aria-label="List view"
              >
                <List size={15} />
              </button>
            </div>
            {canCreate && (
              <button className="btn-accent" onClick={() => navigate('/cases/new')}>
                <Plus size={16} /> New Case
              </button>
            )}
          </>
        }
      />

      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <SearchBar value={keyword} onChange={setKeyword} placeholder="Search by case number or title..." className="sm:w-80" />
        <FilterPanel
          filters={[
            { label: 'All statuses', value: status, onChange: setStatus, options: CASE_STATUSES.map((s) => ({ value: s, label: CASE_STATUS_META[s].label })) },
            { label: 'All priorities', value: priority, onChange: setPriority, options: CASE_PRIORITIES.map((p) => ({ value: p, label: CASE_PRIORITY_META[p].label })) },
          ]}
          onClear={() => { setStatus(undefined); setPriority(undefined); }}
        />
      </div>

      {loading ? (
        <LoadingSpinner label="Loading cases..." />
      ) : error ? (
        <ErrorState message={error} onRetry={load} />
      ) : !pageData?.content?.length ? (
        <EmptyState
          title="No cases found"
          description="Try adjusting your search or filters."
          action={canCreate && <Link to="/cases/new" className="btn-accent">Create the first case</Link>}
        />
      ) : view === 'grid' ? (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {pageData.content.map((c) => (
              <CaseCard key={c.id} caseItem={c} />
            ))}
          </div>
          {pageData.totalPages > 1 && (
            <div className="mt-4 flex items-center justify-center gap-2 text-sm text-slate-500">
              <button className="btn-secondary px-3 py-1" disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>Previous</button>
              Page {page + 1} of {pageData.totalPages}
              <button className="btn-secondary px-3 py-1" disabled={page >= pageData.totalPages - 1} onClick={() => setPage((p) => p + 1)}>Next</button>
            </div>
          )}
        </>
      ) : (
        <DataTable columns={columns} pageData={pageData} onRowClick={(row) => navigate(`/cases/${row.id}`)} onPageChange={setPage} />
      )}
    </div>
  );
}
