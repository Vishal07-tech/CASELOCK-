import { format, formatDistanceToNow } from 'date-fns';

export function formatDate(value, pattern = 'MMM d, yyyy') {
  if (!value) return '-';
  try {
    return format(new Date(value), pattern);
  } catch {
    return '-';
  }
}

export function formatDateTime(value) {
  return formatDate(value, 'MMM d, yyyy h:mm a');
}

export function timeAgo(value) {
  if (!value) return '-';
  try {
    return formatDistanceToNow(new Date(value), { addSuffix: true });
  } catch {
    return '-';
  }
}

export function formatBytes(bytes) {
  if (bytes === null || bytes === undefined) return '-';
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(1)} ${units[i]}`;
}

export function titleCase(value) {
  if (!value) return '';
  return String(value)
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}

export function shortHash(hash, length = 16) {
  if (!hash) return '-';
  return hash.length > length ? `${hash.slice(0, length)}...` : hash;
}
