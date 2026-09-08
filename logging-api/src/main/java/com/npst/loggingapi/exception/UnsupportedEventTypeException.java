package com.npst.loggingapi.exception;

import com.npst.observability.contract.EventType;

/**
 * Raised when a producer sends an event type this layer does not persist.
 *
 * <p>Layer 1 stores application logs only. Audit events belong in the separate,
 * immutable {@code audit_logs} table and error events are not persisted at all,
 * so accepting either here would quietly put the wrong data in the wrong place.
 * Rejecting loudly is the point: a misconfigured service finds out immediately.
 */
public class UnsupportedEventTypeException extends RuntimeException {

    private final transient EventType eventType;

    public UnsupportedEventTypeException(EventType eventType) {
        super("Event type " + eventType + " is not persisted by this layer. "
                + "Only APPLICATION logs are stored in application_logs.");
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }
}
