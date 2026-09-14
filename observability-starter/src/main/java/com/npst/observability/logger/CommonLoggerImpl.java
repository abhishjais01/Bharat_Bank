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

// default logger: masks, adds context, writes to the log file and sends to logging-api
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

    // shortcut for an INFO log
    @Override
    public void logApplication(String message, Map<String, Object> metadata) {
        logApplication(LogLevel.INFO, message, metadata);
    }

    // application log: build, mask, enrich, write, send
    @Override
    public void logApplication(LogLevel level, String message, Map<String, Object> metadata) {

        try {
            LogEvent event = new LogEvent();

            event.setEventType(EventType.APPLICATION);
            event.setLevel(level);
            event.setMessage(message);
            // mask before writing anywhere, the log file is shipped to Loki as it is
            event.setMetadata(masker.mask(metadata));

            // add trace id, bank, service and caller details
            enrich(event);

            // write to the log file
            write(level, event);

            // queue for logging-api
            sink.send(LogEventMapper.toIngestRequest(event));

        } catch (Exception ex) {
            // never let logging break the request
            log.error("Application logging failed : message={}", message, ex);
        }
    }

    // audit with an enum action
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

    // simple audit record from basic fields
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

    // full audit path: enrich, mask, send to logging-api, write to the log file
    @Override
    public void audit(AuditEvent event) {

        try {
            // audit records are always INFO
            event.setLevel(LogLevel.INFO);

            if (event.getMessage() == null) {
                event.setMessage(event.getDescription());
            }

            // add trace id, bank, service and caller details
            enrich(event);

            // mask customer details before the record leaves the service, so the audit
            // table and the log file both get the same masked values
            maskPrivateDetails(event);

            // queue for logging-api
            sink.sendAudit(AuditEventMapper.toIngestRequest(event));

            // same record to the log file
            write(LogLevel.INFO, event);

        } catch (Exception ex) {
            // never let audit logging break the request
            log.error("Audit logging failed : action={}", event.getAction(), ex);
        }
    }

    // hide mobile, account numbers, names and numbers in the URL
    private void maskPrivateDetails(AuditEvent event) {

        event.setMobileNumber(MaskingUtil.maskMobile(event.getMobileNumber()));
        event.setApiEndpoint(MaskingUtil.maskIdentifiersInPath(event.getApiEndpoint()));
        event.setMetadata(masker.mask(event.getMetadata()));
        event.setBusinessContext(masker.mask(event.getBusinessContext()));
        event.setBeforeState(masker.mask(event.getBeforeState()));
        event.setAfterState(masker.mask(event.getAfterState()));
    }

    // error record with stack trace, written to the log file only
    @Override
    public void error(String message, Exception cause) {

        try {
            ErrorEvent event = new ErrorEvent();

            event.setMessage(message);
            event.setException(cause == null ? null : cause.getClass().getName());
            event.setStackTrace(stackTraceOf(cause));

            enrich(event);

            log.error(toJson(event), cause);

        } catch (Exception ex) {
            log.error("Error logging failed : message={}", message, ex);
        }
    }

    // fills the fields every record needs
    private void enrich(LogEvent event) {

        event.setTraceId(MDC.get(properties.getTrace().getMdcKey()));
        event.setBankCode(bankResolver.getCode());
        event.setEnvironment(bankResolver.getEnvironment());
        event.setService(bankResolver.getServiceName());

        // caller details captured by the filter
        event.setChannel(RequestContext.channel());
        event.setDeviceId(RequestContext.deviceId());
        event.setIpAddress(RequestContext.ipAddress());
        event.setCustomerId(RequestContext.customerId());

        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
    }

    // write the JSON at the right log level
    private void write(LogLevel level, LogEvent event) {

        String json = toJson(event);

        switch (level) {
            case DEBUG -> log.debug(json);
            case WARN -> log.warn(json);
            case ERROR -> log.error(json);
            default -> log.info(json);
        }
    }

    // event to JSON text
    private String toJson(LogEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (Exception ex) {
            return "{\"message\":\"log serialization failed\"}";
        }
    }

    // stack trace as text
    private static String stackTraceOf(Exception cause) {

        if (cause == null) {
            return null;
        }

        StringWriter writer = new StringWriter();
        cause.printStackTrace(new PrintWriter(writer));

        return writer.toString();
    }
}
