package com.npst.observability.sink;

import com.npst.observability.contract.AuditIngestRequest;
import com.npst.observability.contract.LogIngestRequest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

// test sink that keeps sent records in memory
public class RecordingLogSink implements LogSink {

    public final List<LogIngestRequest> logs = new CopyOnWriteArrayList<>();
    public final List<AuditIngestRequest> audits = new CopyOnWriteArrayList<>();

    @Override
    public void send(LogIngestRequest request) {
        logs.add(request);
    }

    @Override
    public void sendAudit(AuditIngestRequest request) {
        audits.add(request);
    }

    public LogIngestRequest lastLog() {
        return logs.get(logs.size() - 1);
    }

    public AuditIngestRequest lastAudit() {
        return audits.get(audits.size() - 1);
    }

    public static LogSink behaving(Consumer<Object> handler) {

        return new LogSink() {

            @Override
            public void send(LogIngestRequest request) {
                handler.accept(request);
            }

            @Override
            public void sendAudit(AuditIngestRequest request) {
                handler.accept(request);
            }
        };
    }
}
