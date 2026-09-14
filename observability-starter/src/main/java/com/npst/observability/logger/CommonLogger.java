package com.npst.observability.logger;

import com.npst.observability.contract.LogLevel;
import com.npst.observability.schema.AuditAction;

import java.util.Map;

// main logging API, used by the aspect or directly by services
public interface CommonLogger {

    // application logs
    void logApplication(String message, Map<String, Object> metadata);

    void logApplication(LogLevel level, String message, Map<String, Object> metadata);

    // audit records
    void audit(String actorId,
               String actorType,
               AuditAction action,
               String entity,
               String entityId,
               String description);

    void audit(String actorId,
               String actorType,
               String action,
               String entity,
               String entityId,
               String description);

    void audit(com.npst.observability.schema.AuditEvent event);

    // error with stack trace (log file only)
    void error(String message, Exception cause);
}
