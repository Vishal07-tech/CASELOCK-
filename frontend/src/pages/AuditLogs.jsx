import { useEffect, useState } from 'react';
import auditService from '../services/auditService';
import PageHeader from '../components/PageHeader';
import FilterPanel from '../components/FilterPanel';
import DataTable from '../components/DataTable';
import StatusBadge from '../components/StatusBadge';
import { apiErrorMessage } from '../services/api';
import { formatDateTime } from '../utils/formatters';
import { AUDIT_RESULT_META } from '../utils/constants';

const ACTIONS = [
  'LOGIN', 'LOGOUT', 'LOGIN_FAILED', 'CASE_CREATED', 'CASE_UPDATED', 'CASE_STATUS_CHANGED', 'CASE_CLOSED',
  'CASE_ARCHIVED', 'EVIDENCE_UPLOADED', 'EVIDENCE_ACCESSED', 'EVIDENCE_DOWNLOADED', 'EVIDENCE_TRANSFERRED',
  'EVIDENCE_UPDATED', 'INTEGRITY_VERIFIED', 'USER_CREATED', 'USER_UPDATED', 'USER_DISABLED', 'ROLE_CHANGED',
  'PASSWORD_RESET', 'UNAUTHORIZED_ACCESS_ATTEMPT', 'REPORT_GENERATED',
];

export default function AuditLogs() {
  const [action, setAction] = useState();
  const [result, setResult] = useState();
  const [page, setPage] = useState(0);
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await auditService.search({ action, result, page, size: 15 });
      setPageData(data);
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not load audit logs.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [action, result, page]); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => setPage(0), [action, result]);

  const columns = [
    { key: 'eventTimestamp', header: 'Timestamp', render: (row) => formatDateTime(row.eventTimestamp) },
    { key: 'username', header: 'User' },
    { key: 'action', header: 'Action' },
    { key: 'entityType', header: 'Entity', render: (row) => row.entityType ? `${row.entityType} #${row.entityId}` : '-' },
    { key: 'description', header: 'Description' },
    {
      key: 'result', header: 'Result',
      render: (row) => <StatusBadge label={AUDIT_RESULT_META[row.result]?.label} tone={AUDIT_RESULT_META[row.result]?.tone} />,
    },
    { key: 'ipAddress', header: 'IP Address' },
  ];

  return (
    <div>
      <PageHeader title="Audit Logs" description="A tamper-evident record of every security-relevant action in the system." />

      <div className="mb-4">
        <FilterPanel
          filters={[
            { label: 'All actions', value: action, onChange: setAction, options: ACTIONS.map((a) => ({ value: a, label: a.replace(/_/g, ' ') })) },
            { label: 'All results', value: result, onChange: setResult, options: [{ value: 'SUCCESS', label: 'Success' }, { value: 'FAILURE', label: 'Failure' }, { value: 'DENIED', label: 'Denied' }] },
          ]}
          onClear={() => { setAction(undefined); setResult(undefined); }}
        />
      </div>

      <DataTable
        columns={columns}
        pageData={pageData}
        loading={loading}
        error={error}
        onRetry={load}
        onPageChange={setPage}
        emptyTitle="No audit entries match these filters"
      />
    </div>
  );
}
