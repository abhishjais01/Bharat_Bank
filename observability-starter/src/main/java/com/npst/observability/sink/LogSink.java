package com.npst.observability.sink;

import com.npst.observability.contract.AuditIngestRequest;
import com.npst.observability.contract.LogIngestRequest;

// where finished records are sent
public interface LogSink {

    void send(LogIngestRequest request);

    void sendAudit(AuditIngestRequest request);
}
