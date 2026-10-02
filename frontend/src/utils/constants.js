// Central place for enum -> display-label / color mappings so status colors
// stay consistent everywhere they're rendered (StatusBadge, tables, charts).

export const ROLES = ['ADMIN', 'INVESTIGATOR', 'FORENSIC_ANALYST', 'LEGAL_OFFICER', 'VIEWER'];

export const ROLE_LABELS = {
  ADMIN: 'Station Admin / Chief (Case Assignment & Oversight)',
  INVESTIGATOR: 'Investigator (Police Officer / Detective)',
  FORENSIC_ANALYST: 'Forensic Analyst (Lab Specialist & Hash Verification)',
  LEGAL_OFFICER: 'Legal Officer (Public Prosecutor / Court Presenter)',
  VIEWER: 'Viewer / Judge (Judicial Review & Read-Only)',
};

export const CASE_STATUSES = ['OPEN', 'UNDER_INVESTIGATION', 'EVIDENCE_REVIEW', 'PENDING', 'CLOSED', 'ARCHIVED'];

export const CASE_STATUS_META = {
  OPEN: { label: 'Open', tone: 'blue' },
  UNDER_INVESTIGATION: { label: 'Under Investigation', tone: 'amber' },
  EVIDENCE_REVIEW: { label: 'Evidence Review', tone: 'purple' },
  PENDING: { label: 'Pending', tone: 'slate' },
  CLOSED: { label: 'Closed', tone: 'green' },
  ARCHIVED: { label: 'Archived', tone: 'gray' },
};

export const CASE_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export const CASE_PRIORITY_META = {
  LOW: { label: 'Low', tone: 'slate' },
  MEDIUM: { label: 'Medium', tone: 'blue' },
  HIGH: { label: 'High', tone: 'amber' },
  CRITICAL: { label: 'Critical', tone: 'red' },
};

export const CASE_TYPES = [
  'CRIMINAL', 'CIVIL', 'CYBERCRIME', 'FRAUD', 'INTERNAL_INVESTIGATION', 'REGULATORY_COMPLIANCE', 'OTHER',
];

export const CASE_TYPE_LABELS = {
  CRIMINAL: 'Criminal',
  CIVIL: 'Civil',
  CYBERCRIME: 'Cybercrime',
  FRAUD: 'Fraud',
  INTERNAL_INVESTIGATION: 'Internal Investigation',
  REGULATORY_COMPLIANCE: 'Regulatory Compliance',
  OTHER: 'Other',
};

export const EVIDENCE_CATEGORIES = ['IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT', 'PDF', 'TEXT_FILE', 'ARCHIVE', 'OTHER'];

export const EVIDENCE_CATEGORY_LABELS = {
  IMAGE: 'Image',
  VIDEO: 'Video',
  AUDIO: 'Audio',
  DOCUMENT: 'Document',
  PDF: 'PDF',
  TEXT_FILE: 'Text File',
  ARCHIVE: 'Archive',
  OTHER: 'Other',
};

export const EVIDENCE_STATUS_META = {
  REGISTERED: { label: 'Registered', tone: 'blue' },
  UNDER_ANALYSIS: { label: 'Under Analysis', tone: 'amber' },
  VERIFIED: { label: 'Verified', tone: 'green' },
  TRANSFERRED: { label: 'Transferred', tone: 'purple' },
  SEALED: { label: 'Sealed', tone: 'navy' },
  RETURNED: { label: 'Returned', tone: 'slate' },
  ARCHIVED: { label: 'Archived', tone: 'gray' },
};

export const INTEGRITY_STATUS_META = {
  NOT_YET_VERIFIED: { label: 'Not Yet Verified', tone: 'slate' },
  VERIFIED: { label: 'Integrity Verified', tone: 'green' },
  COMPROMISED: { label: 'Integrity Compromised', tone: 'red' },
};

export const CUSTODY_ACTION_LABELS = {
  COLLECTED: 'Collected',
  UPLOADED: 'Uploaded',
  ACCESSED: 'Accessed',
  TRANSFERRED: 'Transferred',
  ANALYZED: 'Analyzed',
  VERIFIED: 'Verified',
  DOWNLOADED: 'Downloaded',
  RETURNED: 'Returned',
  SEALED: 'Sealed',
  REOPENED: 'Reopened',
  ARCHIVED: 'Archived',
};

export const AUDIT_RESULT_META = {
  SUCCESS: { label: 'Success', tone: 'green' },
  FAILURE: { label: 'Failure', tone: 'red' },
  DENIED: { label: 'Denied', tone: 'red' },
};

export const ACCOUNT_STATUS_META = {
  ACTIVE: { label: 'Active', tone: 'green' },
  DISABLED: { label: 'Disabled', tone: 'gray' },
  LOCKED: { label: 'Locked', tone: 'red' },
};

// Role -> which nav items / actions are visible. Purely a UX convenience;
// the backend re-checks every one of these independently.
export const CAN_CREATE_CASE = ['ADMIN', 'INVESTIGATOR'];
export const CAN_EDIT_CASE = ['ADMIN', 'INVESTIGATOR'];
export const CAN_UPLOAD_EVIDENCE = ['ADMIN', 'INVESTIGATOR', 'FORENSIC_ANALYST'];
export const CAN_VERIFY_EVIDENCE = ['ADMIN', 'FORENSIC_ANALYST'];
export const CAN_TRANSFER_EVIDENCE = ['ADMIN', 'INVESTIGATOR', 'FORENSIC_ANALYST'];
export const CAN_DOWNLOAD_EVIDENCE = ['ADMIN', 'INVESTIGATOR', 'FORENSIC_ANALYST', 'LEGAL_OFFICER'];
export const CAN_MANAGE_USERS = ['ADMIN'];
export const CAN_VIEW_AUDIT_LOGS = ['ADMIN'];
export const CAN_GENERATE_REPORTS = ['ADMIN', 'INVESTIGATOR', 'LEGAL_OFFICER', 'FORENSIC_ANALYST'];
