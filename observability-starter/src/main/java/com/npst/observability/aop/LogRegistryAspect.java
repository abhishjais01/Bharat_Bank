package com.npst.observability.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.npst.observability.contract.LogLevel;
import com.npst.observability.logger.CommonLogger;
import com.npst.observability.model.ActorType;
import com.npst.observability.schema.AuditEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.npst.observability.context.AuditContext;
import com.npst.observability.context.RequestContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

// runs around @LogRegistry methods and turns each call into a log and audit record
@Aspect
public class LogRegistryAspect {

    private static final Logger log = LoggerFactory.getLogger(LogRegistryAspect.class);

    private final CommonLogger commonLogger;
    private final ObjectMapper objectMapper;

    public LogRegistryAspect(CommonLogger commonLogger, ObjectMapper objectMapper) {
        this.commonLogger = commonLogger;
        this.objectMapper = objectMapper;
    }

    // wraps the method: run it, then record what happened
    @Around("@annotation(logRegistry)")
    public Object around(ProceedingJoinPoint joinPoint, LogRegistry logRegistry) throws Throwable {

        // start the timer
        long startedAt = System.nanoTime();

        Object result = null;
        Throwable failure = null;

        // run the real method; any exception is passed on unchanged
        try {
            result = joinPoint.proceed();
            return result;

        } catch (Throwable thrown) {
            failure = thrown;
            throw thrown;

        } finally {
            // runs after success and after failure
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;

            // drain here so values never leak into the next request on this thread
            AuditContext.Details contributed = AuditContext.drain();

            // a logging failure must never break the business call
            try {
                record(joinPoint, logRegistry, result, failure, durationMs, contributed);
            } catch (Exception loggingFailure) {
                log.warn("@LogRegistry could not record {} : {}",
                        logRegistry.action(), loggingFailure.getMessage());
            }
        }
    }

    // writes the application log, then the audit record if needed
    private void record(ProceedingJoinPoint joinPoint,
                        LogRegistry annotation,
                        Object result,
                        Throwable failure,
                        long durationMs,
                        AuditContext.Details contributed) {

        boolean failed = failure != null;

        Map<String, Object> metadata = new LinkedHashMap<>();

        // values from the annotation
        metadata.put("action", annotation.action());
        metadata.put("module", annotation.module());

        if (!annotation.entity().isBlank()) {
            metadata.put("entity", annotation.entity());
        }

        // values from this call
        metadata.put("operation", joinPoint.getSignature().toShortString());
        metadata.put("durationMs", durationMs);
        metadata.put("outcome", failed ? "FAILURE" : "SUCCESS");

        Integer statusCode = statusCodeOf(result);
        if (statusCode != null) {
            metadata.put("statusCode", statusCode);
        }

        // error details when the method threw
        if (failed) {
            metadata.put("exception", failure.getClass().getName());
            metadata.put("responseMessage", failure.getMessage());
        }

        // optional arguments and return value
        if (annotation.logArguments()) {
            metadata.put("arguments", argumentsOf(joinPoint));
        }

        if (annotation.logResult() && !failed) {
            metadata.put("result", asLoggable(result));
        }

        // exceptions listed in warnOn are business rejections (WARN), anything else is ERROR
        LogLevel level = failed ? failureLevel(annotation, failure) : annotation.level();

        // write the application log
        commonLogger.logApplication(level,
                annotation.action() + (failed ? " failed" : " completed"),
                metadata);

        // audit record only for audited actions
        if (annotation.audit()) {
            writeAudit(annotation, failed, failure, statusCode, durationMs, contributed);
        }
    }

    // builds the audit record: who did what, to which record, and the result
    private void writeAudit(LogRegistry annotation,
                            boolean failed,
                            Throwable failure,
                            Integer statusCode,
                            long durationMs,
                            AuditContext.Details contributed) {

        AuditEvent event = new AuditEvent();

        // actor: customer from the request header, else from AuditContext, else SYSTEM
        String customerId = RequestContext.customerId() != null
                ? RequestContext.customerId()
                : contributed == null ? null : contributed.getCustomerId();

        event.setActorId(orDefault(customerId, ActorType.SYSTEM.name()));
        event.setActorType(customerId == null
                ? ActorType.SYSTEM.name()
                : ActorType.CUSTOMER.name());

        // what was done
        event.setAction(annotation.action());
        event.setModule(annotation.module());
        event.setEntity(annotation.entity().isBlank()
                ? annotation.module()
                : annotation.entity());
        event.setDescription(annotation.action() + (failed ? " failed" : " completed"));

        // result of the call
        event.setStatusCode(statusCode);
        event.setResponseMessage(failed ? failure.getMessage() : null);
        event.setDurationMs(durationMs);

        // which API was called
        HttpServletRequest request = currentRequest();

        if (request != null) {
            event.setApiEndpoint(request.getRequestURI());
            event.setApiMethod(request.getMethod());
        }

        // details the controller added through AuditContext
        if (contributed != null) {
            event.setEntityId(contributed.getEntityId());
            event.setAmount(contributed.getAmount());
            event.setCurrency(contributed.getCurrency());
            event.setBusinessRef(contributed.getBusinessRef());
            event.setMobileNumber(contributed.getMobileNumber());
            event.setBusinessContext(contributed.getBusinessContext());
            event.setBeforeState(contributed.getBeforeState());
            event.setAfterState(contributed.getAfterState());

            if (event.getCustomerId() == null) {
                event.setCustomerId(contributed.getCustomerId());
            }
        }

        // mask and send the audit record
        commonLogger.audit(event);
    }

    // current HTTP request, or null outside a web request
    private static HttpServletRequest currentRequest() {

        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        return attributes instanceof ServletRequestAttributes servlet
                ? servlet.getRequest()
                : null;
    }

    // WARN for expected business errors, ERROR for everything else
    private static LogLevel failureLevel(LogRegistry annotation, Throwable failure) {

        for (Class<? extends Throwable> businessFailure : annotation.warnOn()) {
            if (businessFailure.isInstance(failure)) {
                return LogLevel.WARN;
            }
        }

        return LogLevel.ERROR;
    }

    // HTTP status, only known when the method returns ResponseEntity
    private static Integer statusCodeOf(Object result) {
        return result instanceof ResponseEntity<?> response
                ? response.getStatusCode().value()
                : null;
    }

    // method arguments as name -> value
    private Map<String, Object> argumentsOf(ProceedingJoinPoint joinPoint) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] names = signature.getParameterNames();
        Object[] values = joinPoint.getArgs();

        Map<String, Object> arguments = new LinkedHashMap<>();

        for (int i = 0; i < values.length; i++) {
            String name = names != null && i < names.length ? names[i] : "arg" + i;
            arguments.put(name, asLoggable(values[i]));
        }

        return arguments;
    }

    // turns objects into maps so their fields can be masked
    private Object asLoggable(Object value) {

        if (value == null || isSimple(value)) {
            return value;
        }

        try {
            // convert to a map so the masker can see field names like otp
            return objectMapper.convertValue(value, Map.class);
        } catch (Exception notConvertible) {
            return value.getClass().getSimpleName();
        }
    }

    // plain values that can be logged as they are
    private static boolean isSimple(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>;
    }

    // fallback when the value is empty
    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
