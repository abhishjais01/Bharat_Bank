package com.npst.observability.logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.npst.observability.client.LoggingClient;
import com.npst.observability.config.ObservabilityProperties;
import com.npst.observability.config.bank.BankResolver;
import com.npst.observability.contract.EventType;
import com.npst.observability.contract.LogLevel;
import com.npst.observability.mapper.LogEventMapper;
import com.npst.observability.schema.AuditAction;
import com.npst.observability.schema.AuditEvent;
import com.npst.observability.schema.ErrorEvent;
import com.npst.observability.schema.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;

/**
 * Default {@link CommonLogger}.
 *
 * <p>The enrichment is the point: a developer in a banking service writes a
 * message and some business metadata, and traceId, bankCode, environment,
 * service and timestamp are attached here. No controller should ever pass its
 * own bankCode or service name.
 *
 * <p>No longer a {@code @Component} - it is contributed by
 * {@code ObservabilityAutoConfiguration}, so it is found regardless of the
 * host application's base package.
 */
public class CommonLoggerImpl implements CommonLogger {

    private static final Logger log = LoggerFactory.getLogger(CommonLoggerImpl.class);

    private final ObjectMapper mapper;
    private final BankResolver bankResolver;
    private final LoggingClient loggingClient;
    private final ObservabilityProperties properties;

    public CommonLoggerImpl(ObjectMapper mapper,
                            BankResolver bankResolver,
                            LoggingClient loggingClient,
                            ObservabilityProperties properties) {
        this.mapper = mapper;
        this.bankResolver = bankResolver;
        this.loggingClient = loggingClient;
        this.properties = properties;
    }

    @Override
    public void logApplication(String message, Map<String, Object> metadata) {
        logApplication(LogLevel.INFO, message, metadata);
    }

    @Override
    public void logApplication(LogLevel level, String message, Map<String, Object> metadata) {

        try {
            LogEvent event = new LogEvent();

            event.setEventType(EventType.APPLICATION);
            event.setLevel(level);
            event.setMessage(message);
            event.setMetadata(metadata);

            enrich(event);

            write(level, event);

            loggingClient.send(LogEventMapper.toIngestRequest(event));

        } catch (Exception ex) {
            log.error("Application logging failed : message={}", message, ex);
        }
    }

    @Override
    public void audit(String actorId,
                      String actorType,
                      AuditAction action,
                      String entity,
                      String entityId,
                      String description) {

        try {
            AuditEvent event = new AuditEvent();

            event.setActorId(actorId);
            event.setActorType(actorType);
            event.setAction(action);
            event.setEntity(entity);
            event.setEntityId(entityId);
            event.setDescription(description);
            event.setMessage(description);
            event.setLevel(LogLevel.INFO);

            enrich(event);

            write(LogLevel.INFO, event);

            // Deliberately not shipped to logging-api. Layer 1 persists
            // application logs only; audit gets its own immutable table in
            // Layer 2. Until then audit events reach Loki through the file.

        } catch (Exception ex) {
            log.error("Audit logging failed : action={}", action, ex);
        }
    }

    @Override
    public void error(String message, Exception cause) {

        try {
            ErrorEvent event = new ErrorEvent();

            event.setMessage(message);
            event.setException(cause == null ? null : cause.getClass().getName());
            event.setStackTrace(stackTraceOf(cause));

            enrich(event);

            log.error(toJson(event), cause);

            // Error persistence is Layer 1's explicit non-goal; the stack
            // trace still reaches the file and Loki.

        } catch (Exception ex) {
            log.error("Error logging failed : message={}", message, ex);
        }
    }

    /** Stamps platform identity onto an event. The whole point of the SDK. */
    private void enrich(LogEvent event) {

        event.setTraceId(MDC.get(properties.getTrace().getMdcKey()));
        event.setBankCode(bankResolver.getCode());
        event.setEnvironment(bankResolver.getEnvironment());
        event.setService(bankResolver.getServiceName());

        if (event.getTimestamp() == null) {
            event.setTimestamp(java.time.Instant.now());
        }
    }

    private void write(LogLevel level, LogEvent event) {

        String json = toJson(event);

        switch (level) {
            case DEBUG -> log.debug(json);
            case WARN -> log.warn(json);
            case ERROR -> log.error(json);
            default -> log.info(json);
        }
    }

    private String toJson(LogEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (Exception ex) {
            return "{\"message\":\"log serialization failed\"}";
        }
    }

    private static String stackTraceOf(Exception cause) {

        if (cause == null) {
            return null;
        }

        java.io.StringWriter writer = new java.io.StringWriter();
        cause.printStackTrace(new java.io.PrintWriter(writer));

        return writer.toString();
    }
}
