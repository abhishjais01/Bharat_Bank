package com.npst.observability.exception;

import java.time.LocalDateTime;

public class ErrorResponse {

    private String traceId;
    private String errorCode;
    private String message;
    private int status;
    private LocalDateTime timestamp;

    public ErrorResponse() {
    }

    public ErrorResponse(String traceId,
                         String errorCode,
                         String message,
                         int status,
                         LocalDateTime timestamp) {
        this.traceId = traceId;
        this.errorCode = errorCode;
        this.message = message;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
