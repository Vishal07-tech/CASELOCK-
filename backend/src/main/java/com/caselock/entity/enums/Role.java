package com.caselock.entity.enums;

/**
 * System roles. Authorization rules (see SecurityConfig and the various
 * @PreAuthorize annotations on controllers) are built around these five
 * roles:
 *
 * ADMIN            - manage users/roles, view all cases, view audit logs
 * INVESTIGATOR     - create cases, add evidence, view assigned cases, initiate transfers
 * FORENSIC_ANALYST - analyze evidence, verify integrity, add forensic findings
 * LEGAL_OFFICER    - view authorized evidence, review cases, generate reports
 * VIEWER           - read-only authorized access
 */
public enum Role {
    ADMIN,
    INVESTIGATOR,
    FORENSIC_ANALYST,
    LEGAL_OFFICER,
    VIEWER
}
