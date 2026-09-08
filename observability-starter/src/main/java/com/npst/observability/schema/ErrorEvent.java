package com.npst.observability.schema;

import com.npst.observability.contract.EventType;
import com.npst.observability.contract.LogLevel;

public class ErrorEvent extends LogEvent {

    private String exception;
    private String stackTrace;

    public ErrorEvent() {
        setEventType(EventType.ERROR);
        setLevel(LogLevel.ERROR);
    }

    // Getters & Setters
    public String getException() {
        return exception;
    }
    public void setException(String exception) {
        this.exception = exception;
    }
    public String getStackTrace() {
        return stackTrace;
    }
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
}