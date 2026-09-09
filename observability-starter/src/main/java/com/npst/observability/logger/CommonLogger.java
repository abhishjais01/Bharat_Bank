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

    /** Records who did what to which entity, using the standard action set. */
    void audit(String actorId,
               String actorType,
               AuditAction action,
               String entity,
               String entityId,
               String description);

    /**
     * Same, with a free-form action.
     *
     * <p>AuditAction cannot enumerate every action a bank performs - each new
     * domain would need a new enum constant in this shared library. @LogRegistry
     * declares its action as a string for exactly that reason.
     */
    void audit(String actorId,
               String actorType,
               String action,
               String entity,
               String entityId,
               String description);

    /** Records a failure together with its stack trace. */
    void error(String message, Exception cause);
}
