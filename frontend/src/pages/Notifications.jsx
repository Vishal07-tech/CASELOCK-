import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, CheckCheck } from 'lucide-react';
import notificationService from '../services/notificationService';
import PageHeader from '../components/PageHeader';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import { useNotifications } from '../context/NotificationContext';
import { apiErrorMessage } from '../services/api';
import { timeAgo } from '../utils/formatters';
import clsx from 'clsx';

export default function Notifications() {
  const navigate = useNavigate();
  const { refreshUnreadCount } = useNotifications();
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await notificationService.list({ size: 30 });
      setPageData(data);
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not load notifications.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleClick = async (notification) => {
    if (!notification.read) {
      await notificationService.markRead(notification.id);
      refreshUnreadCount();
      load();
    }
    if (notification.relatedEvidenceId) navigate(`/evidence/${notification.relatedEvidenceId}`);
    else if (notification.relatedCaseId) navigate(`/cases/${notification.relatedCaseId}`);
  };

  const handleMarkAllRead = async () => {
    await notificationService.markAllRead();
    refreshUnreadCount();
    load();
  };

  return (
    <div>
      <PageHeader
        title="Notifications"
        description="Case assignments, transfers, and integrity alerts relevant to you."
        actions={
          <button className="btn-secondary" onClick={handleMarkAllRead}>
            <CheckCheck size={15} /> Mark all as read
          </button>
        }
      />

      {loading ? (
        <LoadingSpinner label="Loading notifications..." />
      ) : error ? (
        <ErrorState message={error} onRetry={load} />
      ) : !pageData?.content?.length ? (
        <EmptyState icon={Bell} title="You're all caught up" description="New notifications will appear here." />
      ) : (
        <ul className="space-y-2">
          {pageData.content.map((n) => (
            <li key={n.id}>
              <button
                onClick={() => handleClick(n)}
                className={clsx(
                  'card flex w-full items-start justify-between gap-3 p-4 text-left transition-colors hover:bg-slate-50',
                  !n.read && 'border-l-4 border-l-accent-600 bg-accent-50/40'
                )}
              >
                <div>
                  <p className="font-medium text-slate-800">{n.title}</p>
                  <p className="mt-0.5 text-sm text-slate-500">{n.message}</p>
                </div>
                <span className="shrink-0 text-xs text-slate-400">{timeAgo(n.createdAt)}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
