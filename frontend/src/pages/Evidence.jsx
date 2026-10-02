import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import evidenceService from '../services/evidenceService';
import PageHeader from '../components/PageHeader';
import SearchBar from '../components/SearchBar';
import FilterPanel from '../components/FilterPanel';
import EvidenceCard from '../components/EvidenceCard';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import useDebounce from '../hooks/useDebounce';
import { apiErrorMessage } from '../services/api';
import { EVIDENCE_CATEGORIES, EVIDENCE_CATEGORY_LABELS } from '../utils/constants';

export default function Evidence() {
  const [searchParams] = useSearchParams();
  const caseId = searchParams.get('caseId') || undefined;

  const [keyword, setKeyword] = useState('');
  const debouncedKeyword = useDebounce(keyword);
  const [category, setCategory] = useState();
  const [page, setPage] = useState(0);

  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await evidenceService.list({ keyword: debouncedKeyword || undefined, category, caseId, page, size: 12 });
      setPageData(data);
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not load evidence.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [debouncedKeyword, category, caseId, page]); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => setPage(0), [debouncedKeyword, category, caseId]);

  return (
    <div>
      <PageHeader title="Evidence" description="Every evidence item you're authorized to view, with its integrity status." />

      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <SearchBar value={keyword} onChange={setKeyword} placeholder="Search by evidence number, file name or hash..." className="sm:w-96" />
        <FilterPanel
          filters={[
            { label: 'All categories', value: category, onChange: setCategory, options: EVIDENCE_CATEGORIES.map((c) => ({ value: c, label: EVIDENCE_CATEGORY_LABELS[c] })) },
          ]}
          onClear={() => setCategory(undefined)}
        />
      </div>

      {loading ? (
        <LoadingSpinner label="Loading evidence..." />
      ) : error ? (
        <ErrorState message={error} onRetry={load} />
      ) : !pageData?.content?.length ? (
        <EmptyState title="No evidence found" description="Try adjusting your search or filters, or open a case to upload evidence." />
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {pageData.content.map((e) => <EvidenceCard key={e.id} evidence={e} />)}
          </div>
          {pageData.totalPages > 1 && (
            <div className="mt-4 flex items-center justify-center gap-2 text-sm text-slate-500">
              <button className="btn-secondary px-3 py-1" disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>Previous</button>
              Page {page + 1} of {pageData.totalPages}
              <button className="btn-secondary px-3 py-1" disabled={page >= pageData.totalPages - 1} onClick={() => setPage((p) => p + 1)}>Next</button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
