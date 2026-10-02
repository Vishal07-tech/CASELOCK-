import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  FolderLock, FileStack, ShieldCheck, ShieldAlert, ArrowRightLeft, FolderOpen,
} from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, CartesianGrid,
} from 'recharts';
import dashboardService from '../services/dashboardService';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorState from '../components/ErrorState';
import EmptyState from '../components/EmptyState';
import StatusBadge from '../components/StatusBadge';
import { apiErrorMessage } from '../services/api';
import { formatDateTime, titleCase } from '../utils/formatters';
import { AUDIT_RESULT_META } from '../utils/constants';

const PIE_COLORS = ['#2563eb', '#16a34a', '#f59e0b', '#dc2626', '#7c3aed', '#0891b2', '#64748b', '#334e68'];

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await dashboardService.getStats();
      setStats(data);
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not load dashboard statistics.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  if (loading) return <LoadingSpinner fullPage label="Loading dashboard..." />;
  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!stats) return null;

  const caseStatusData = Object.entries(stats.casesByStatus)
    .filter(([, count]) => count > 0)
    .map(([status, count]) => ({ name: titleCase(status), value: count }));

  const evidenceCategoryData = Object.entries(stats.evidenceByCategory)
    .filter(([, count]) => count > 0)
    .map(([category, count]) => ({ name: titleCase(category), value: count }));

  return (
    <div>
      <PageHeader title="Dashboard" description="A live overview of case activity, evidence integrity, and recent events." />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Total Cases" value={stats.totalCases} icon={FolderLock} tone="navy" />
        <StatCard label="Active Cases" value={stats.activeCases} icon={FolderOpen} tone="blue" />
        <StatCard label="Closed Cases" value={stats.closedCases} icon={ShieldCheck} tone="green" />
        <StatCard label="Total Evidence" value={stats.totalEvidence} icon={FileStack} tone="navy" />
        <StatCard label="Verified Evidence" value={stats.verifiedEvidence} icon={ShieldCheck} tone="green" />
        <StatCard label="Integrity Alerts" value={stats.integrityAlerts} icon={ShieldAlert} tone="red"
                  hint={stats.integrityAlerts > 0 ? 'Requires attention' : 'All clear'} />
        <StatCard label="Pending Transfers" value={stats.pendingTransfers} icon={ArrowRightLeft} tone="amber" />
        <StatCard label="Security Alerts" value={stats.securityAlerts?.length ?? 0} icon={ShieldAlert} tone="red" />
      </div>

      <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="card p-4 lg:col-span-2">
          <h2 className="mb-3 text-sm font-semibold text-slate-700">Monthly Case &amp; Evidence Activity</h2>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={stats.monthlyCaseActivity}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="month" tick={{ fontSize: 11 }} stroke="#94a3b8" />
              <YAxis tick={{ fontSize: 11 }} stroke="#94a3b8" allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="caseCount" name="Cases" fill="#2563eb" radius={[4, 4, 0, 0]} />
              <Bar dataKey="evidenceCount" name="Evidence" fill="#16a34a" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="card p-4">
          <h2 className="mb-3 text-sm font-semibold text-slate-700">Evidence Integrity Status</h2>
          {evidenceCategoryData.length === 0 ? (
            <EmptyState title="No evidence yet" />
          ) : (
            <ResponsiveContainer width="100%" height={220}>
              <PieChart>
                <Pie data={caseStatusData} dataKey="value" nameKey="name" innerRadius={45} outerRadius={75} paddingAngle={2}>
                  {caseStatusData.map((entry, idx) => (
                    <Cell key={entry.name} fill={PIE_COLORS[idx % PIE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          )}
          <p className="mt-1 text-center text-xs text-slate-400">Cases by status</p>
        </div>
      </div>

      <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div className="card p-4">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-700">Recent Cases</h2>
            <Link to="/cases" className="text-xs font-medium text-accent-600 hover:underline">View all</Link>
          </div>
          {stats.recentCases.length === 0 ? (
            <EmptyState title="No cases yet" />
          ) : (
            <ul className="divide-y divide-slate-100">
              {stats.recentCases.map((c) => (
                <li key={c.id} className="py-2.5">
                  <Link to={`/cases/${c.id}`} className="flex items-center justify-between gap-3 hover:text-accent-600">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium">{c.title}</p>
                      <p className="text-xs text-slate-400">{c.caseNumber}</p>
                    </div>
                    <span className="shrink-0 text-xs text-slate-400">{formatDateTime(c.updatedAt)}</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="card p-4">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-700">Recent Evidence</h2>
            <Link to="/evidence" className="text-xs font-medium text-accent-600 hover:underline">View all</Link>
          </div>
          {stats.recentEvidence.length === 0 ? (
            <EmptyState title="No evidence yet" />
          ) : (
            <ul className="divide-y divide-slate-100">
              {stats.recentEvidence.map((e) => (
                <li key={e.id} className="py-2.5">
                  <Link to={`/evidence/${e.id}`} className="flex items-center justify-between gap-3 hover:text-accent-600">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium">{e.fileName}</p>
                      <p className="text-xs text-slate-400">{e.evidenceNumber}</p>
                    </div>
                    <span className="shrink-0 text-xs text-slate-400">{formatDateTime(e.uploadDate)}</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div className="card p-4">
          <h2 className="mb-3 text-sm font-semibold text-slate-700">Recent Activity</h2>
          {stats.recentActivity.length === 0 ? (
            <EmptyState title="No recent activity" />
          ) : (
            <ul className="space-y-2">
              {stats.recentActivity.map((log) => (
                <li key={log.id} className="flex items-start justify-between gap-3 text-sm">
                  <span className="text-slate-600">{log.description}</span>
                  <span className="shrink-0 text-xs text-slate-400">{formatDateTime(log.eventTimestamp)}</span>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="card p-4">
          <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-700">
            <ShieldAlert size={15} className="text-red-500" /> Security Alerts
          </h2>
          {stats.securityAlerts.length === 0 ? (
            <EmptyState title="No unauthorized access attempts" description="Nothing to review right now." />
          ) : (
            <ul className="space-y-2">
              {stats.securityAlerts.map((log) => (
                <li key={log.id} className="flex items-start justify-between gap-3 text-sm">
                  <span className="text-slate-600">{log.description}</span>
                  <StatusBadge label={AUDIT_RESULT_META[log.result]?.label} tone={AUDIT_RESULT_META[log.result]?.tone} />
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
