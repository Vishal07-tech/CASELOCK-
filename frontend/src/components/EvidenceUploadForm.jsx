import { useRef, useState } from 'react';
import { UploadCloud, File as FileIcon, X, Loader2 } from 'lucide-react';
import evidenceService from '../services/evidenceService';
import { apiErrorMessage } from '../services/api';
import { notify } from './Toast';
import { formatBytes } from '../utils/formatters';
import { EVIDENCE_CATEGORIES, EVIDENCE_CATEGORY_LABELS } from '../utils/constants';

/**
 * Full evidence upload experience: drag-and-drop or browse, file preview
 * (name/size/type), description + category, progress feedback, and the
 * File Selected -> Validate -> Upload -> Hash -> Metadata -> Custody Event ->
 * Audit Log -> Success pipeline (all server-side; this form just drives it).
 */
export default function EvidenceUploadForm({ caseId, onSuccess }) {
  const inputRef = useRef(null);
  const [file, setFile] = useState(null);
  const [dragOver, setDragOver] = useState(false);
  const [category, setCategory] = useState('DOCUMENT');
  const [description, setDescription] = useState('');
  const [progress, setProgress] = useState(0);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  const pickFile = (selected) => {
    if (!selected) return;
    setFile(selected);
    setError('');
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    pickFile(e.dataTransfer.files?.[0]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!file) {
      setError('Please select a file to upload.');
      return;
    }
    setUploading(true);
    setError('');
    setProgress(0);
    try {
      await evidenceService.upload(file, { caseId: Number(caseId), evidenceCategory: category, description }, setProgress);
      notify.success('Evidence registered and SHA-256 hash computed successfully.');
      setFile(null);
      setDescription('');
      onSuccess?.();
    } catch (err) {
      setError(apiErrorMessage(err, 'Something went wrong while uploading the evidence. Please try again.'));
    } finally {
      setUploading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {error && <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}

      <div
        onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
        onDragLeave={() => setDragOver(false)}
        onDrop={handleDrop}
        onClick={() => inputRef.current?.click()}
        className={`flex cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed p-8 text-center transition-colors ${
          dragOver ? 'border-accent-500 bg-accent-50' : 'border-slate-300 hover:border-accent-400'
        }`}
      >
        <input ref={inputRef} type="file" className="hidden" onChange={(e) => pickFile(e.target.files?.[0])} />
        <UploadCloud className="text-slate-400" size={28} />
        <p className="text-sm text-slate-600">
          <span className="font-medium text-accent-600">Click to browse</span> or drag and drop a file here
        </p>
        <p className="text-xs text-slate-400">Any evidence file type; server-side limits and type checks apply.</p>
      </div>

      {file && (
        <div className="flex items-center justify-between rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
          <div className="flex min-w-0 items-center gap-2">
            <FileIcon size={16} className="shrink-0 text-slate-400" />
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-slate-700">{file.name}</p>
              <p className="text-xs text-slate-400">{formatBytes(file.size)} &middot; {file.type || 'unknown type'}</p>
            </div>
          </div>
          <button type="button" onClick={() => setFile(null)} className="text-slate-400 hover:text-slate-600">
            <X size={15} />
          </button>
        </div>
      )}

      <div>
        <label className="label">Evidence Category</label>
        <select className="input" value={category} onChange={(e) => setCategory(e.target.value)}>
          {EVIDENCE_CATEGORIES.map((c) => <option key={c} value={c}>{EVIDENCE_CATEGORY_LABELS[c]}</option>)}
        </select>
      </div>

      <div>
        <label className="label">Description</label>
        <textarea className="input" rows={3} value={description} onChange={(e) => setDescription(e.target.value)}
                  placeholder="Briefly describe this evidence item and how it was collected." />
      </div>

      {uploading && (
        <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100">
          <div className="h-full bg-accent-600 transition-all" style={{ width: `${progress}%` }} />
        </div>
      )}

      <div className="flex justify-end gap-2 pt-1">
        <button type="submit" className="btn-accent" disabled={uploading}>
          {uploading && <Loader2 size={16} className="animate-spin" />}
          {uploading ? `Uploading (${progress}%)...` : 'Register Evidence'}
        </button>
      </div>
    </form>
  );
}
