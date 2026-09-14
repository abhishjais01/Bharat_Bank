package com.npst.loggingapi.exception;

import com.npst.observability.contract.EventType;

// thrown when a non-APPLICATION event is sent to /api/v1/logs
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
