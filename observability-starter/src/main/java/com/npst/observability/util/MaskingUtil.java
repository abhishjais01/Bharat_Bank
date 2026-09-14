package com.npst.observability.util;

import java.util.regex.Pattern;

// small masking rules for different kinds of values
public final class MaskingUtil {

    private static final int VISIBLE_SUFFIX = 4;

    private static final Pattern LONG_NUMBER_IN_PATH = Pattern.compile("(?<=/)\\d{9,}(?=/|\\?|$)");

    private MaskingUtil() {
    }

    // 918273645510 -> XXXXXXXX5510
    public static String maskAccountNumber(String accountNumber) {
        return keepLast(accountNumber, VISIBLE_SUFFIX);
    }

    // 4111111111111111 -> XXXXXXXXXXXX1111
    public static String maskCardNumber(String cardNumber) {
        return keepLast(cardNumber, VISIBLE_SUFFIX);
    }

    // 9876543210 -> 98XXXX3210
    public static String maskMobile(String mobile) {

        if (isTooShort(mobile, 10)) {
            return keepLast(mobile, 2);
        }

        return mobile.substring(0, 2) + "XXXX" + mobile.substring(6);
    }

    // ABCDE1234F -> ABCDEXXXXF
    public static String maskPan(String pan) {

        if (isTooShort(pan, 10)) {
            return keepLast(pan, 1);
        }

        return pan.substring(0, 5) + "XXXX" + pan.substring(9);
    }

    // keeps the last 4 digits
    public static String maskAadhaar(String aadhaar) {
        return keepLast(aadhaar, VISIBLE_SUFFIX);
    }

    // rajesh@bank.in -> rXXXXX@bank.in
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

    // Rajesh Amin -> RXXXXX AXXX
    public static String maskName(String name) {

        if (name == null || name.isBlank()) {
            return name;
        }

        StringBuilder masked = new StringBuilder(name.length());
        boolean wordStart = true;

        for (char c : name.toCharArray()) {
            if (Character.isWhitespace(c)) {
                masked.append(c);
                wordStart = true;
            } else {
                masked.append(wordStart ? c : 'X');
                wordStart = false;
            }
        }

        return masked.toString();
    }

    // account or card numbers used as path segments, e.g. /accounts/918273645510/balance
    public static String maskIdentifiersInPath(String path) {

        if (path == null) {
            return null;
        }

        return LONG_NUMBER_IN_PATH.matcher(path)
                .replaceAll(match -> keepLast(match.group(), VISIBLE_SUFFIX));
    }

    // keep the last few characters, replace the rest with X
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
