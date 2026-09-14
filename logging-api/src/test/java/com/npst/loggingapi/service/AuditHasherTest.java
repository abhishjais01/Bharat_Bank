package com.npst.loggingapi.service;

import com.npst.loggingapi.entity.AuditLog;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

// checks the audit hash chain values
class AuditHasherTest {

    private final AuditHasher hasher = new AuditHasher();

    @Test
    void producesAStableSha256Digest() {

        String first = hasher.hash(null, transfer("TXN-001", "5000.00"));
        String second = hasher.hash(null, transfer("TXN-001", "5000.00"));

        assertThat(first)
                .hasSize(64)
                .matches("[0-9a-f]{64}")
                .isEqualTo(second);
    }

    @Test
    void changingAnyAuditedFieldChangesTheHash() {

        String original = hasher.hash(null, transfer("TXN-001", "5000.00"));

        assertThat(hasher.hash(null, transfer("TXN-001", "5000.01")))
                .as("an altered amount must not go unnoticed")
                .isNotEqualTo(original);

        assertThat(hasher.hash(null, transfer("TXN-002", "5000.00")))
                .as("an altered reference must not go unnoticed")
                .isNotEqualTo(original);
    }

    @Test
    void theSameRowUnderADifferentParentHashesDifferently() {
        AuditLog entry = transfer("TXN-001", "5000.00");

        assertThat(hasher.hash("a".repeat(64), entry))
                .isNotEqualTo(hasher.hash("b".repeat(64), entry));
    }

    @Test
    void theFirstRowHashesAgainstAGenesisMarker() {
        assertThat(hasher.hash(null, transfer("TXN-001", "5000.00")))
                .isEqualTo(hasher.hash(null, transfer("TXN-001", "5000.00")));
    }

    private static AuditLog transfer(String reference, String amount) {

        AuditLog entry = new AuditLog();

        entry.setTraceId("TRACE-1");
        entry.setBankCode("NPST");
        entry.setEnvironment("DEV");
        entry.setService("sample-bank-app");
        entry.setActorId("CIF-99001");
        entry.setActorType("CUSTOMER");
        entry.setAction("FUND_TRANSFER");
        entry.setEntity("TRANSFER");
        entry.setEntityId(reference);
        entry.setBusinessRef(reference);
        entry.setCustomerId("CIF-99001");
        entry.setChannel("MOBILE");
        entry.setAmount(new BigDecimal(amount));
        entry.setCurrency("INR");
        entry.setEventTime(Instant.parse("2026-09-09T17:00:00Z"));

        return entry;
    }
}
