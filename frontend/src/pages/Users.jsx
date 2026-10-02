import { useEffect, useState } from 'react';
import { Plus, UserX, UserCheck } from 'lucide-react';
import userService from '../services/userService';
import PageHeader from '../components/PageHeader';
import SearchBar from '../components/SearchBar';
import FilterPanel from '../components/FilterPanel';
import DataTable from '../components/DataTable';
import StatusBadge from '../components/StatusBadge';
import Modal from '../components/Modal';
import ConfirmationDialog from '../components/ConfirmationDialog';
import useDebounce from '../hooks/useDebounce';
import { apiErrorMessage } from '../services/api';
import { notify } from '../components/Toast';
import { formatDateTime } from '../utils/formatters';
import { ROLES, ROLE_LABELS, ACCOUNT_STATUS_META } from '../utils/constants';

export default function Users() {
  const [keyword, setKeyword] = useState('');
  const debouncedKeyword = useDebounce(keyword);
  const [role, setRole] = useState();
  const [page, setPage] = useState(0);
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [pendingAction, setPendingAction] = useState(null); // { type, user }

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await userService.list({ keyword: debouncedKeyword || undefined, role, page, size: 12 });
      setPageData(data);
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not load users.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [debouncedKeyword, role, page]); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => setPage(0), [debouncedKeyword, role]);

  const handleConfirmAction = async () => {
    if (!pendingAction) return;
    try {
      if (pendingAction.type === 'disable') {
        await userService.disable(pendingAction.user.id);
        notify.success('User account disabled.');
      } else if (pendingAction.type === 'enable') {
        await userService.enable(pendingAction.user.id);
        notify.success('User account enabled.');
      }
      setPendingAction(null);
      load();
    } catch (err) {
      notify.error(apiErrorMessage(err, 'Could not complete this action.'));
    }
  };

  const columns = [
    { key: 'fullName', header: 'Name' },
    { key: 'username', header: 'Username' },
    { key: 'email', header: 'Email' },
    { key: 'role', header: 'Role', render: (row) => ROLE_LABELS[row.role] },
    {
      key: 'accountStatus', header: 'Status',
      render: (row) => <StatusBadge label={ACCOUNT_STATUS_META[row.accountStatus]?.label} tone={ACCOUNT_STATUS_META[row.accountStatus]?.tone} />,
    },
    { key: 'lastLoginAt', header: 'Last Login', render: (row) => formatDateTime(row.lastLoginAt) },
    {
      key: 'actions', header: 'Actions',
      render: (row) => (
        <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
          {row.accountStatus === 'DISABLED' ? (
            <button className="text-emerald-600 hover:underline" onClick={() => setPendingAction({ type: 'enable', user: row })}>
              <UserCheck size={15} />
            </button>
          ) : (
            <button className="text-red-600 hover:underline" onClick={() => setPendingAction({ type: 'disable', user: row })}>
              <UserX size={15} />
            </button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="User Management"
        description="Add, disable and manage roles for every account in CaseLock."
        actions={
          <button className="btn-accent" onClick={() => setCreateOpen(true)}>
            <Plus size={16} /> New User
          </button>
        }
      />

      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <SearchBar value={keyword} onChange={setKeyword} placeholder="Search by name, username or email..." className="sm:w-80" />
        <FilterPanel
          filters={[{ label: 'All roles', value: role, onChange: setRole, options: ROLES.map((r) => ({ value: r, label: ROLE_LABELS[r] })) }]}
          onClear={() => setRole(undefined)}
        />
      </div>

      <DataTable columns={columns} pageData={pageData} loading={loading} error={error} onRetry={load} onPageChange={setPage} emptyTitle="No users found" />

      <CreateUserModal open={createOpen} onClose={() => setCreateOpen(false)} onCreated={() => { setCreateOpen(false); load(); }} />

      <ConfirmationDialog
        open={Boolean(pendingAction)}
        title={pendingAction?.type === 'disable' ? 'Disable this account?' : 'Enable this account?'}
        message={
          pendingAction?.type === 'disable'
            ? `${pendingAction?.user?.fullName} will no longer be able to log in until re-enabled.`
            : `${pendingAction?.user?.fullName} will be able to log in again.`
        }
        danger={pendingAction?.type === 'disable'}
        onConfirm={handleConfirmAction}
        onCancel={() => setPendingAction(null)}
      />
    </div>
  );
}

function CreateUserModal({ open, onClose, onCreated }) {
  const [form, setForm] = useState({ username: '', email: '', password: '', fullName: '', badgeNumber: '', department: '', role: 'VIEWER' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const update = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const handleSubmit = async () => {
    setSubmitting(true);
    setError('');
    try {
      await userService.create(form);
      notify.success('User created successfully.');
      onCreated();
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not create this user.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Create New User"
      footer={
        <>
          <button className="btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn-accent" onClick={handleSubmit} disabled={submitting}>{submitting ? 'Creating...' : 'Create User'}</button>
        </>
      }
    >
      {error && <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}
      <div className="space-y-3">
        <div className="grid grid-cols-2 gap-3">
          <input className="input" placeholder="Full name" value={form.fullName} onChange={update('fullName')} />
          <input className="input" placeholder="Username" value={form.username} onChange={update('username')} />
        </div>
        <input className="input" placeholder="Email address" type="email" value={form.email} onChange={update('email')} />
        <input className="input" placeholder="Temporary password" type="password" value={form.password} onChange={update('password')} />
        <div className="grid grid-cols-2 gap-3">
          <input className="input" placeholder="Badge / ID number" value={form.badgeNumber} onChange={update('badgeNumber')} />
          <input className="input" placeholder="Department" value={form.department} onChange={update('department')} />
        </div>
        <select className="input" value={form.role} onChange={update('role')}>
          {ROLES.map((r) => <option key={r} value={r}>{ROLE_LABELS[r]}</option>)}
        </select>
      </div>
    </Modal>
  );
}
