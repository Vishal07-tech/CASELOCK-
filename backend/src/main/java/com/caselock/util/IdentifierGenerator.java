package com.caselock.util;

import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates human-readable, sequential-looking case and evidence numbers,
 * e.g. CASE-2026-000123 / EVD-2026-000456. Uniqueness is still verified
 * against the database by the calling service before it is persisted.
 */
public final class IdentifierGenerator {

    private static final AtomicLong CASE_SEQUENCE = new AtomicLong(1);
    private static final AtomicLong EVIDENCE_SEQUENCE = new AtomicLong(1);

    private IdentifierGenerator() {
    }

    public static String nextCaseNumber() {
        int year = Year.now().getValue();
        return "CASE-%d-%06d".formatted(year, CASE_SEQUENCE.getAndIncrement());
    }

    public static String nextEvidenceNumber() {
        int year = Year.now().getValue();
        return "EVD-%d-%06d".formatted(year, EVIDENCE_SEQUENCE.getAndIncrement());
    }
}
