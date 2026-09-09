package com.npst.observability.sink;

import com.npst.observability.contract.AuditIngestRequest;
import com.npst.observability.contract.LogIngestRequest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Test double that keeps whatever it is handed.
 *
 * <p>LogSink stopped being a functional interface when audit got its own
 * method, so tests need this rather than a lambda - and it is more useful
 * anyway, since it separates the two streams and lets a test assert that an
 * audit event did <em>not</em> travel the application-log path.
 */
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

    /** Applies the same behaviour to both streams - for queue and failure tests. */
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
