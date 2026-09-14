package com.npst.observability.contract;

// kind of log record; logging-api stores only APPLICATION in application_logs
public enum EventType {
    APPLICATION,
    AUDIT,
    ERROR,
    SECURITY,
    BUSINESS
}
