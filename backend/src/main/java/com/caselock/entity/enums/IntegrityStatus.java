package com.caselock.entity.enums;

/**
 * Result of comparing an evidence item's stored original SHA-256 hash
 * against a freshly computed hash of the current file content.
 *
 * IMPORTANT: a hash match confirms the digital content is byte-identical to
 * what was recorded at registration time. It does NOT, by itself, prove the
 * evidence is authentic or was lawfully collected - that determination
 * still relies on the full chain-of-custody record.
 */
public enum IntegrityStatus {
    NOT_YET_VERIFIED,
    VERIFIED,
    COMPROMISED
}
