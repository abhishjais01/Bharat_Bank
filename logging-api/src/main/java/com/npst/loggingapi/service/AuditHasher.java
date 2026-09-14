package com.npst.loggingapi.service;

import com.npst.loggingapi.entity.AuditLog;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.StringJoiner;

// builds the SHA-256 hash that links each audit row to the previous one
@Component
public class AuditHasher {

    private static final String ALGORITHM = "SHA-256";
    private static final String FIELD_SEPARATOR = "|";

    // hash = SHA-256(previous hash | important fields of this row)
    public String hash(String previousHash, AuditLog entry) {

        StringJoiner canonical = new StringJoiner(FIELD_SEPARATOR);

        // previous hash first, GENESIS for the very first row
        canonical.add(previousHash == null ? "GENESIS" : previousHash);
        canonical.add(nullSafe(entry.getTraceId()));
        canonical.add(nullSafe(entry.getBankCode()));
        canonical.add(nullSafe(entry.getEnvironment()));
        canonical.add(nullSafe(entry.getService()));
        canonical.add(nullSafe(entry.getActorId()));
        canonical.add(nullSafe(entry.getActorType()));
        canonical.add(nullSafe(entry.getAction()));
        canonical.add(nullSafe(entry.getEntity()));
        canonical.add(nullSafe(entry.getEntityId()));
        canonical.add(nullSafe(entry.getCustomerId()));
        canonical.add(nullSafe(entry.getChannel()));
        canonical.add(nullSafe(entry.getIpAddress()));
        canonical.add(nullSafe(entry.getBusinessRef()));
        canonical.add(entry.getAmount() == null ? "" : entry.getAmount().toPlainString());
        canonical.add(nullSafe(entry.getCurrency()));
        canonical.add(nullSafe(entry.getStatusCode()));
        canonical.add(nullSafe(entry.getEventTime()));

        // hash the joined text
        return digest(canonical.toString());
    }

    // SHA-256 as hex text
    private static String digest(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));

        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    // empty text for null
    private static String nullSafe(Object value) {
        return value == null ? "" : value.toString();
    }
}
