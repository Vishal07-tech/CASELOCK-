import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import caseService from '../services/caseService';
import custodyService from '../services/custodyService';
import PageHeader from '../components/PageHeader';
import Timeline from '../components/Timeline';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import { apiErrorMessage } from '../services/api';
import { notify } from '../components/Toast';

export default function ChainOfCustody() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [cases, setCases] = useState([]);
  const [caseId, setCaseId] = useState(searchParams.get('caseId') || '');
  const [events, setEvents] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    caseService.list({ size: 100 }).then((d) => setCases(d.content)).catch(() => {});
  }, []);

  useEffect(() => {
    if (!caseId) {
      setEvents(null);
      return;
    }
    setLoading(true);
    custodyService.forCase(caseId, { size: 100 })
      .then((d) => setEvents(d.content))
      .catch((err) => notify.error(apiErrorMessage(err, 'Could not load the chain of custody.')))
      .finally(() => setLoading(false));
  }, [caseId]);

  const handleSelectCase = (value) => {
    setCaseId(value);
    setSearchParams(value ? { caseId: value } : {});
  };

  return (
    <div>
      <PageHeader
        title="Chain of Custody"
        description="Select a case to see the complete custody timeline for every piece of evidence within it."
      />

      <div className="mb-5 max-w-sm">
        <select className="input" value={caseId} onChange={(e) => handleSelectCase(e.target.value)}>
          <option value="">Select a case...</option>
          {cases.map((c) => (
            <option key={c.id} value={c.id}>{c.caseNumber} - {c.title}</option>
          ))}
        </select>
      </div>

      {!caseId ? (
        <EmptyState title="Select a case above" description="Its full chain-of-custody timeline will appear here." />
      ) : loading ? (
        <LoadingSpinner label="Loading chain of custody..." />
      ) : !events?.length ? (
        <EmptyState title="No custody events for this case yet" />
      ) : (
        <Timeline events={events} />
      )}
    </div>
  );
}
