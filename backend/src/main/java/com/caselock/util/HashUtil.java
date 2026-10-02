package com.caselock.util;

import com.caselock.exception.BadRequestException;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Computes SHA-256 hashes of evidence files.
 *
 * Note: a matching hash confirms the file's digital content is byte-for-byte
 * identical to what was recorded at registration time. It does not, on its
 * own, prove the evidence was lawfully collected or is otherwise
 * "authentic" - that determination relies on the full chain-of-custody
 * record alongside this integrity check.
 */
public final class HashUtil {

    private static final String ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 8192;

    private HashUtil() {
    }

    public static String sha256(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException ex) {
            throw new BadRequestException("Unable to compute the file's SHA-256 hash: " + ex.getMessage());
        }
    }

    public static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            return toHex(digest.digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new BadRequestException("Unable to compute the file's SHA-256 hash: " + ex.getMessage());
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
