import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import userService from '../services/userService';
import PageHeader from '../components/PageHeader';
import DataTable from '../components/DataTable';
import StatusBadge from '../components/StatusBadge';
import { ROLE_LABELS } from '../utils/constants';
import { formatDateTime } from '../utils/formatters';

export default function Profile() {
  const { user } = useAuth();
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    setError('');
    try {
      const data = await userService.loginHistory(user.id, { page, size: 8 });
      setPageData(data);
    } catch (err) {
      // Non-admin users may not be able to view login history via this admin
      // endpoint; fail quietly rather than showing an alarming error.
      setError('');
      setPageData(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [user, page]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!user) return null;

  const columns = [
    { key: 'attemptedAt', header: 'When', render: (row) => formatDateTime(row.attemptedAt) },
    { key: 'successful', header: 'Result', render: (row) => <StatusBadge label={row.successful ? 'Success' : 'Failed'} tone={row.successful ? 'green' : 'red'} /> },
    { key: 'ipAddress', header: 'IP Address' },
    { key: 'failureReason', header: 'Notes' },
  ];

  return (
    <div>
      <PageHeader title="My Profile" description="Your account details and recent sign-in activity." />

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="card p-5 lg:col-span-1">
          <div className="mb-4 flex flex-col items-center text-center">
            <div className="mb-3 flex h-16 w-16 items-center justify-center rounded-full bg-navy-900 text-xl font-semibold text-white">
              {user.fullName?.charAt(0)}
            </div>
            <p className="font-semibold text-slate-900">{user.fullName}</p>
            <p className="text-sm text-slate-400">{ROLE_LABELS[user.role]}</p>
          </div>
          <div className="space-y-2 text-sm">
            <Row label="Username" value={user.username} />
            <Row label="Email" value={user.email} />
            <Row label="Badge Number" value={user.badgeNumber} />
            <Row label="Department" value={user.department} />
            <Row label="Phone" value={user.phoneNumber} />
            <Row label="Last Login" value={formatDateTime(user.lastLoginAt)} />
            <Row label="Last Login IP" value={user.lastLoginIp} />
            <Row label="Member Since" value={formatDateTime(user.createdAt)} />
          </div>
        </div>

        <div className="lg:col-span-2">
          <h3 className="mb-3 text-sm font-semibold text-slate-700">Recent Login History</h3>
          {pageData ? (
            <DataTable columns={columns} pageData={pageData} loading={loading} error={error} onRetry={load} onPageChange={setPage} />
          ) : (
            <p className="text-sm text-slate-400">Login history is available to administrators.</p>
          )}
        </div>
      </div>
    </div>
  );
}

function Row({ label, value }) {
  return (
    <div className="flex items-center justify-between border-b border-slate-100 pb-2 last:border-0">
      <span className="text-slate-400">{label}</span>
      <span className="font-medium text-slate-700">{value || '-'}</span>
    </div>
  );
}
