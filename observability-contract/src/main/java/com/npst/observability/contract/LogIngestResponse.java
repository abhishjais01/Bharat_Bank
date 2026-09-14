package com.npst.observability.contract;

// id of the stored row, returned inside ApiResponse
public class LogIngestResponse {

    private Long logId;

    public LogIngestResponse() {
    }

    public LogIngestResponse(Long logId) {
        this.logId = logId;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }
}
