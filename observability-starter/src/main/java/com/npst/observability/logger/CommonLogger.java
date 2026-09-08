package com.npst.observability.logger;

import com.npst.observability.contract.LogLevel;
import com.npst.observability.schema.AuditAction;

import java.util.Map;

/**
 * The single entry point a banking microservice uses to record what happened.
 *
 * <p>Callers supply a message and business metadata only. Correlation id, bank
 * code, environment, service name and timestamp are attached by the platform.
 */
public interface CommonLogger {

    /** Records an application event at INFO. */
    void logApplication(String message, Map<String, Object> metadata);

    /**
     * Records an application event at a chosen severity - a rejected transfer
     * is a WARN, not an INFO, and support filters on exactly that.
     */
    void logApplication(LogLevel level, String message, Map<String, Object> metadata);

    /** Records who did what to which entity. Persisted in Layer 2. */
    void audit(String actorId,
               String actorType,
               AuditAction action,
               String entity,
               String entityId,
               String description);

    /** Records a failure together with its stack trace. */
    void error(String message, Exception cause);
}
