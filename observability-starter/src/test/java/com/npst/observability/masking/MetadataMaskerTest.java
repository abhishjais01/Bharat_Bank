package com.npst.observability.masking;

import com.npst.observability.config.ObservabilityProperties;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The PRD's Security NFR forbids OTPs, tokens and biometric data in plaintext
 * logs. Metadata is a free-form map, so nothing but this class stands between
 * a developer and an OTP on disk. These are the tests that hold that line.
 */
class MetadataMaskerTest {

    private final MetadataMasker masker =
            new MetadataMasker(new ObservabilityProperties.Masking());

    @Test
    void redactsSecretsEntirely() {

        Map<String, Object> masked = masker.mask(Map.of(
                "otp", "483920",
                "mpin", "1234",
                "token", "eyJhbGciOiJIUzI1NiJ9.abc"));

        assertThat(masked.values()).containsOnly("***REDACTED***");
        assertThat(masked.toString()).doesNotContain("483920", "1234", "eyJhbGci");
    }

    @Test
    void masksIdentifiersButLeavesEnoughToSupportACustomerCall() {

        Map<String, Object> masked = masker.mask(Map.of(
                "accountNumber", "918273645510",
                "mobile", "9876543210",
                "pan", "ABCDE1234F",
                "email", "rajesh.amin@bharatbank.in"));

        assertThat(masked.get("accountNumber")).isEqualTo("XXXXXXXX5510");
        assertThat(masked.get("mobile")).isEqualTo("98XXXX3210");
        assertThat(masked.get("pan")).isEqualTo("ABCDEXXXXF");
        assertThat(masked.get("email")).isEqualTo("rXXXXX@bharatbank.in");
    }

    @Test
    void keyMatchingIgnoresCaseAndSeparators() {

        Map<String, Object> masked = masker.mask(Map.of(
                "ACCOUNT_NUMBER", "918273645510",
                "account-number", "918273645511",
                "OTP", "483920"));

        assertThat(masked.get("ACCOUNT_NUMBER")).isEqualTo("XXXXXXXX5510");
        assertThat(masked.get("account-number")).isEqualTo("XXXXXXXX5511");
        assertThat(masked.get("OTP")).isEqualTo("***REDACTED***");
    }

    @Test
    void doesNotMatchOnSubstrings() {
        // "pin" must not swallow "shipping" - the reason matching is exact.
        Map<String, Object> masked = masker.mask(Map.of("shippingAddress", "MG Road"));

        assertThat(masked.get("shippingAddress")).isEqualTo("MG Road");
    }

    @Test
    void reachesIntoNestedStructures() {

        Map<String, Object> beneficiary = new LinkedHashMap<>();
        beneficiary.put("name", "Rajesh Amin");
        beneficiary.put("accountNumber", "918273645510");

        Map<String, Object> masked = masker.mask(Map.of(
                "transferType", "IMPS",
                "beneficiary", beneficiary,
                "otpAttempts", List.of(Map.of("otp", "483920"))));

        assertThat(masked.get("transferType")).isEqualTo("IMPS");

        assertThat(masked.toString())
                .as("a nested account number or OTP must not survive")
                .doesNotContain("918273645510", "483920")
                .contains("XXXXXXXX5510", "***REDACTED***");
    }

    @Test
    void leavesTheCallersMapUntouched() {
        // A logging call must never mutate the business object it was handed.
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("otp", "483920");

        masker.mask(original);

        assertThat(original.get("otp")).isEqualTo("483920");
    }

    @Test
    void canBeDisabledForALocalDebuggingSession() {

        ObservabilityProperties.Masking off = new ObservabilityProperties.Masking();
        off.setEnabled(false);

        assertThat(new MetadataMasker(off).mask(Map.of("otp", "483920")))
                .containsEntry("otp", "483920");
    }

    @Test
    void toleratesNullAndEmptyMetadata() {
        assertThat(masker.mask(null)).isNull();
        assertThat(masker.mask(Map.of())).isEmpty();
    }
}
