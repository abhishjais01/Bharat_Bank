package com.npst.observability.contract;

/**
 * Body of a successful ingest, carried inside {@link ApiResponse#getData()}.
 *
 * <p>The id lets a caller - or a support engineer replaying a failure - point
 * at the exact stored row rather than searching for it.
 */
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
