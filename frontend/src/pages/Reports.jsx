import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Download, FileBarChart } from 'lucide-react';
import caseService from '../services/caseService';
import reportService from '../services/reportService';
import PageHeader from '../components/PageHeader';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import StatusBadge from '../components/StatusBadge';
import { apiErrorMessage } from '../services/api';
import { notify } from '../components/Toast';
import { formatDateTime, shortHash } from '../utils/formatters';
import { INTEGRITY_STATUS_META } from '../utils/constants';

export default function Reports() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [cases, setCases] = useState([]);
  const [caseId, setCaseId] = useState(searchParams.get('caseId') || '');
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    caseService.list({ size: 100 }).then((d) => setCases(d.content)).catch(() => {});
  }, []);

  useEffect(() => {
    if (!caseId) {
      setReport(null);
      return;
    }
    setLoading(true);
    reportService.get(caseId)
      .then(setReport)
      .catch((err) => notify.error(apiErrorMessage(err, 'Could not generate this report.')))
      .finally(() => setLoading(false));
  }, [caseId]);

  const handleSelectCase = (value) => {
    setCaseId(value);
    setSearchParams(value ? { caseId: value } : {});
  };

  const handleDownloadPdf = async () => {
    setDownloading(true);
    try {
      await reportService.downloadPdf(caseId, report?.caseInfo?.caseNumber);
    } catch (err) {
      notify.error(apiErrorMessage(err, 'Could not download the PDF report.'));
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="Case Reports"
        description="Generate a formal report covering case information, evidence, chain of custody, and audit history."
        actions={
          report && (
            <button className="btn-accent" onClick={handleDownloadPdf} disabled={downloading}>
              <Download size={15} /> {downloading ? 'Preparing PDF...' : 'Download PDF'}
            </button>
          )
        }
      />

      <div className="mb-5 max-w-sm">
        <select className="input" value={caseId} onChange={(e) => handleSelectCase(e.target.value)}>
          <option value="">Select a case...</option>
          {cases.map((c) => <option key={c.id} value={c.id}>{c.caseNumber} - {c.title}</option>)}
        </select>
      </div>

      {!caseId ? (
        <EmptyState icon={FileBarChart} title="Select a case above" description="Its full report will be generated here." />
      ) : loading ? (
        <LoadingSpinner label="Generating report..." />
      ) : !report ? null : (
        <div className="space-y-5">
          <section className="card p-5">
            <h3 className="mb-3 text-sm font-semibold text-slate-700">Case Information</h3>
            <div className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
              <Row label="Case Number" value={report.caseInfo.caseNumber} />
              <Row label="Title" value={report.caseInfo.title} />
              <Row label="Status" value={report.caseInfo.status} />
              <Row label="Priority" value={report.caseInfo.priority} />
              <Row label="Investigator" value={report.caseInfo.investigator?.fullName} />
              <Row label="Location" value={report.caseInfo.location} />
            </div>
          </section>

          <section className="card p-5">
            <h3 className="mb-3 text-sm font-semibold text-slate-700">Evidence Summary ({report.evidenceSummary.length})</h3>
            {report.evidenceSummary.length === 0 ? <p className="text-sm text-slate-400">No evidence registered.</p> : (
              <div className="overflow-x-auto">
                <table className="table-base w-full">
                  <thead><tr><th>Evidence #</th><th>File Name</th><th>Status</th><th>Integrity</th></tr></thead>
                  <tbody>
                    {report.evidenceSummary.map((e) => (
                      <tr key={e.id}>
                        <td>{e.evidenceNumber}</td>
                        <td>{e.fileName}</td>
                        <td>{e.status}</td>
                        <td><StatusBadge label={INTEGRITY_STATUS_META[e.integrityStatus]?.label} tone={INTEGRITY_STATUS_META[e.integrityStatus]?.tone} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section className="card p-5">
            <h3 className="mb-3 text-sm font-semibold text-slate-700">Integrity Verification Results</h3>
            {report.integrityResults.length === 0 ? <p className="text-sm text-slate-400">No verification results yet.</p> : (
              <ul className="space-y-2 text-sm">
                {report.integrityResults.map((r) => (
                  <li key={r.evidenceId} className="flex items-center justify-between border-b border-slate-100 pb-2 last:border-0">
                    <span>{r.evidenceNumber} - {shortHash(r.originalSha256, 20)}</span>
                    <StatusBadge label={INTEGRITY_STATUS_META[r.integrityStatus]?.label} tone={INTEGRITY_STATUS_META[r.integrityStatus]?.tone} />
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="card p-5">
            <h3 className="mb-3 text-sm font-semibold text-slate-700">Chain of Custody ({report.chainOfCustody.length} events)</h3>
            {report.chainOfCustody.length === 0 ? <p className="text-sm text-slate-400">No custody events recorded.</p> : (
              <ul className="space-y-1.5 text-sm">
                {report.chainOfCustody.map((e) => (
                  <li key={e.id} className="flex justify-between text-slate-600">
                    <span>{e.action}: {e.fromUserName || 'N/A'} to {e.toUserName || 'N/A'}</span>
                    <span className="text-xs text-slate-400">{formatDateTime(e.eventTimestamp)}</span>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="card p-5">
            <h3 className="mb-3 text-sm font-semibold text-slate-700">Audit History ({report.auditHistory.length})</h3>
            {report.auditHistory.length === 0 ? <p className="text-sm text-slate-400">No audit entries.</p> : (
              <ul className="space-y-1.5 text-sm">
                {report.auditHistory.map((a) => (
                  <li key={a.id} className="flex justify-between text-slate-600">
                    <span>{a.description}</span>
                    <span className="text-xs text-slate-400">{formatDateTime(a.eventTimestamp)}</span>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <p className="text-xs text-slate-400">
            Generated {formatDateTime(report.generatedAt)} by {report.generatedBy}
          </p>
        </div>
      )}
    </div>
  );
}

function Row({ label, value }) {
  return (
    <div>
      <p className="text-xs text-slate-400">{label}</p>
      <p className="font-medium text-slate-700">{value ?? '-'}</p>
    </div>
  );
}
