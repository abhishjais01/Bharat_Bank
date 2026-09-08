package com.npst.observability.util;

/**
 * Partial masking rules for the banking identifiers that legitimately appear
 * in a log line.
 *
 * <p>Each rule keeps just enough for a support engineer to match the value
 * against the customer in front of them, and no more. A fully redacted account
 * number would make half of production support impossible; a full one is a
 * regulatory problem.
 */
public final class MaskingUtil {

    private static final int VISIBLE_SUFFIX = 4;

    private MaskingUtil() {
    }

    /** {@code 918273645510 -> XXXXXXXX5510} */
    public static String maskAccountNumber(String accountNumber) {
        return keepLast(accountNumber, VISIBLE_SUFFIX);
    }

    /** {@code 4111111111111111 -> XXXXXXXXXXXX1111} */
    public static String maskCardNumber(String cardNumber) {
        return keepLast(cardNumber, VISIBLE_SUFFIX);
    }

    /** {@code 9876543210 -> 98XXXX3210} */
    public static String maskMobile(String mobile) {

        if (isTooShort(mobile, 10)) {
            return keepLast(mobile, 2);
        }

        return mobile.substring(0, 2) + "XXXX" + mobile.substring(6);
    }

    /** {@code ABCDE1234F -> ABCDEXXXXF} */
    public static String maskPan(String pan) {

        if (isTooShort(pan, 10)) {
            return keepLast(pan, 1);
        }

        return pan.substring(0, 5) + "XXXX" + pan.substring(9);
    }

    /** {@code 123456789012 -> XXXXXXXX9012} */
    public static String maskAadhaar(String aadhaar) {
        return keepLast(aadhaar, VISIBLE_SUFFIX);
    }

    /** {@code rajesh.amin@bank.in -> rXXXXX@bank.in} */
    public static String maskEmail(String email) {

        if (email == null) {
            return null;
        }

        int at = email.indexOf('@');

        if (at <= 1) {
            return keepLast(email, 0);
        }

        return email.charAt(0) + "XXXXX" + email.substring(at);
    }

    /**
     * Fallback for any value matched by key but with no specific rule: keep
     * the last {@code visible} characters, replace the rest with X.
     */
    public static String keepLast(String value, int visible) {

        if (value == null || value.isBlank()) {
            return value;
        }

        if (value.length() <= visible) {
            return "X".repeat(value.length());
        }

        return "X".repeat(value.length() - visible)
                + value.substring(value.length() - visible);
    }

    private static boolean isTooShort(String value, int expectedLength) {
        return value == null || value.length() != expectedLength;
    }
}
