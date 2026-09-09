package com.npst.observability.logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.npst.observability.config.ObservabilityProperties;
import com.npst.observability.config.bank.BankResolver;
import com.npst.observability.contract.EventType;
import com.npst.observability.context.RequestContext;
import com.npst.observability.contract.LogLevel;
import com.npst.observability.mapper.AuditEventMapper;
import com.npst.observability.mapper.LogEventMapper;
import com.npst.observability.masking.MetadataMasker;
import com.npst.observability.util.MaskingUtil;
import com.npst.observability.schema.AuditAction;
import com.npst.observability.schema.AuditEvent;
import com.npst.observability.schema.ErrorEvent;
import com.npst.observability.schema.LogEvent;
import com.npst.observability.sink.LogSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Map;

/**
 * Default {@link CommonLogger}.
 *
 * <p>The enrichment is the point: a developer in a banking service writes a
 * message and some business metadata, and traceId, bankCode, environment,
 * service and timestamp are attached here. No controller should ever pass its
 * own bankCode or service name.
 *
 * <p>Masking happens before the line is written anywhere, not just before it
 * is sent. The file is tailed straight into Loki, so an unmasked OTP written
 * to disk has already leaked.
 *
 * <p>Not a {@code @Component} - it is contributed by
 * {@code ObservabilityAutoConfiguration}, so it is found regardless of the
 * host application's base package.
 */
public class CommonLoggerImpl implements CommonLogger {

    private static final Logger log = LoggerFactory.getLogger(CommonLoggerImpl.class);

    private final ObjectMapper mapper;
    private final BankResolver bankResolver;
    private final LogSink sink;
    private final MetadataMasker masker;
    private final ObservabilityProperties properties;

    public CommonLoggerImpl(ObjectMapper mapper,
                            BankResolver bankResolver,
                            LogSink sink,
                            MetadataMasker masker,
                            ObservabilityProperties properties) {
        this.mapper = mapper;
        this.bankResolver = bankResolver;
        this.sink = sink;
        this.masker = masker;
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
            event.setMetadata(masker.mask(metadata));

            enrich(event);

            write(level, event);

            sink.send(LogEventMapper.toIngestRequest(event));

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

        audit(actorId, actorType, action == null ? null : action.name(),
                entity, entityId, description);
    }

    @Override
    public void audit(String actorId,
                      String actorType,
                      String action,
                      String entity,
                      String entityId,
                      String description) {

        AuditEvent event = new AuditEvent();

        event.setActorId(actorId);
        event.setActorType(actorType);
        event.setAction(action);
        event.setEntity(entity);
        event.setEntityId(entityId);
        event.setDescription(description);

        audit(event);
    }

    @Override
    public void audit(AuditEvent event) {

        try {
            event.setLevel(LogLevel.INFO);

            if (event.getMessage() == null) {
                event.setMessage(event.getDescription());
            }

            enrich(event);

            // Order matters. The wire copy is taken first and carries identity
            // intact - the audit table is a compliance record, and a regulator
            // asking who moved money cannot work with XXXXXXXX5510.
            sink.sendAudit(AuditEventMapper.toIngestRequest(event));

            // The file copy is then masked. It is tailed straight into Loki,
            // which has no access control of its own, so unmasked identity must
            // not reach it. Same event, two audiences, two rules.
            maskForFile(event);

            write(LogLevel.INFO, event);

        } catch (Exception ex) {
            log.error("Audit logging failed : action={}", event.getAction(), ex);
        }
    }

    /**
     * Scrubs the copy that goes to disk. The mapper has already taken an
     * unmasked snapshot for the audit table, so mutating the event here is safe.
     */
    private void maskForFile(AuditEvent event) {

        event.setMobileNumber(MaskingUtil.maskMobile(event.getMobileNumber()));
        event.setMetadata(masker.mask(event.getMetadata()));
        event.setBusinessContext(masker.mask(event.getBusinessContext()));
        event.setBeforeState(masker.mask(event.getBeforeState()));
        event.setAfterState(masker.mask(event.getAfterState()));
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

        // Captured once per request at the edge, so every log carries it -
        // including logs written by code that knows nothing about this SDK.
        event.setChannel(RequestContext.channel());
        event.setDeviceId(RequestContext.deviceId());
        event.setIpAddress(RequestContext.ipAddress());
        event.setCustomerId(RequestContext.customerId());

        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
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

        StringWriter writer = new StringWriter();
        cause.printStackTrace(new PrintWriter(writer));

        return writer.toString();
    }
}
