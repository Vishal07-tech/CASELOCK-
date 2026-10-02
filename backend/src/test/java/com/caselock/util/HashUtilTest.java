package com.caselock.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the core cryptographic-integrity primitive CaseLock is built on:
 * identical content must always hash the same, and any change to the bytes
 * must change the hash (this is exactly what "INTEGRITY COMPROMISED"
 * detection in EvidenceService relies on).
 */
class HashUtilTest {

    @Test
    void sameContentProducesSameHash() {
        String content = "CaseLock evidence content";
        String hash1 = HashUtil.sha256(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        String hash2 = HashUtil.sha256(content.getBytes(StandardCharsets.UTF_8));

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 -> 32 bytes -> 64 hex chars
    }

    @Test
    void differentContentProducesDifferentHash() {
        String original = "original evidence bytes";
        String tampered = "original evidence bytes ";

        String originalHash = HashUtil.sha256(original.getBytes(StandardCharsets.UTF_8));
        String tamperedHash = HashUtil.sha256(tampered.getBytes(StandardCharsets.UTF_8));

        assertThat(originalHash).isNotEqualTo(tamperedHash);
    }

    @Test
    void knownVectorMatchesStandardSha256() {
        // SHA-256("abc") is a well-known published test vector.
        String hash = HashUtil.sha256("abc".getBytes(StandardCharsets.UTF_8));
        assertThat(hash).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
