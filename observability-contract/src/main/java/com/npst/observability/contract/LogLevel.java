package com.npst.observability.contract;

/**
 * Severity of an application log line.
 *
 * <p>Part of the wire contract: the SDK emits these names and logging-api
 * validates against the same enum, so a typo cannot reach the database.
 */
public enum LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}
