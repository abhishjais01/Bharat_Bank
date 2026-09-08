package com.npst.observability.contract;

/**
 * The kind of event a log line represents.
 *
 * <p>Layer 1 persists {@link #APPLICATION} only. AUDIT and ERROR are declared
 * so the contract does not have to change when Layer 2 turns them on; they are
 * written to file and shipped to Loki today, but rejected by logging-api.
 */
public enum EventType {
    APPLICATION,
    AUDIT,
    ERROR,
    SECURITY,
    BUSINESS
}
