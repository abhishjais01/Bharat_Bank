package com.npst.observability.contract;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

// common response wrapper returned by every logging-api endpoint
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    public static final String SUCCESS = "SUCCESS";
    public static final String ERROR = "ERROR";

    private String status;
    private String message;
    private String traceId;
    private Instant timestamp;
    private T data;

    private List<String> errors;

    public ApiResponse() {
    }

    private ApiResponse(String status, String message, String traceId, T data, List<String> errors) {
        this.status = status;
        this.message = message;
        this.traceId = traceId;
        this.data = data;
        this.errors = errors;
        this.timestamp = Instant.now();
    }

    // builds a success response
    public static <T> ApiResponse<T> success(String message, String traceId, T data) {
        return new ApiResponse<>(SUCCESS, message, traceId, data, null);
    }

    // builds an error response with the list of problems
    public static <T> ApiResponse<T> error(String message, String traceId, List<String> errors) {
        return new ApiResponse<>(ERROR, message, traceId, null, errors);
    }

    // getters and setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
