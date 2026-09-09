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

/**
 * Turns a {@link LogRegistry} annotation into a complete log record.
 *
 * <p>Two rules govern everything here:
 *
 * <ol>
 *   <li><b>The business method's behaviour is never altered.</b> A thrown
 *       exception is re-thrown exactly as it was; the return value is passed
 *       through untouched. This aspect observes, it does not participate.</li>
 *   <li><b>A logging failure never becomes a banking failure.</b> Everything
 *       the aspect does for itself is wrapped, so a bug in log assembly cannot
 *       take down a fund transfer.</li>
 * </ol>
 */
@Aspect
public class LogRegistryAspect {

    private static final Logger log = LoggerFactory.getLogger(LogRegistryAspect.class);

    private final CommonLogger commonLogger;
    private final ObjectMapper objectMapper;

    public LogRegistryAspect(CommonLogger commonLogger, ObjectMapper objectMapper) {
        this.commonLogger = commonLogger;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(logRegistry)")
    public Object around(ProceedingJoinPoint joinPoint, LogRegistry logRegistry) throws Throwable {

        long startedAt = System.nanoTime();

        Object result = null;
        Throwable failure = null;

        try {
            result = joinPoint.proceed();
            return result;

        } catch (Throwable thrown) {
            failure = thrown;
            throw thrown;

        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;

            // Drained here, not inside record(), so it is cleared even if
            // recording itself fails. A leftover amount attaching to the next
            // unrelated transaction on this thread would be worse than none.
            AuditContext.Details contributed = AuditContext.drain();

            try {
                record(joinPoint, logRegistry, result, failure, durationMs, contributed);
            } catch (Exception loggingFailure) {
                log.warn("@LogRegistry could not record {} : {}",
                        logRegistry.action(), loggingFailure.getMessage());
            }
        }
    }

    private void record(ProceedingJoinPoint joinPoint,
                        LogRegistry annotation,
                        Object result,
                        Throwable failure,
                        long durationMs,
                        AuditContext.Details contributed) {

        boolean failed = failure != null;

        Map<String, Object> metadata = new LinkedHashMap<>();

        // --- constant: fixed by the annotation, true of every invocation ----
        metadata.put("action", annotation.action());
        metadata.put("module", annotation.module());

        if (!annotation.entity().isBlank()) {
            metadata.put("entity", annotation.entity());
        }

        // --- runtime: only knowable during this particular execution --------
        metadata.put("operation", joinPoint.getSignature().toShortString());
        metadata.put("durationMs", durationMs);
        metadata.put("outcome", failed ? "FAILURE" : "SUCCESS");

        Integer statusCode = statusCodeOf(result);
        if (statusCode != null) {
            metadata.put("statusCode", statusCode);
        }

        if (failed) {
            metadata.put("exception", failure.getClass().getName());
            metadata.put("responseMessage", failure.getMessage());
        }

        if (annotation.logArguments()) {
            metadata.put("arguments", argumentsOf(joinPoint));
        }

        if (annotation.logResult() && !failed) {
            metadata.put("result", asLoggable(result));
        }

        // Masking happens inside CommonLogger, so anything sensitive that came
        // in through arguments or the result is scrubbed before it is written.
        LogLevel level = failed ? LogLevel.ERROR : annotation.level();

        commonLogger.logApplication(level,
                annotation.action() + (failed ? " failed" : " completed"),
                metadata);

        if (annotation.audit()) {
            writeAudit(annotation, failed, failure, statusCode, durationMs, contributed);
        }
    }

    /**
     * Builds the audit record. Everything the annotation cannot know - who the
     * actor was, which endpoint was called, what the system answered, how long
     * it took - is filled in from the request context and this execution.
     */
    private void writeAudit(LogRegistry annotation,
                            boolean failed,
                            Throwable failure,
                            Integer statusCode,
                            long durationMs,
                            AuditContext.Details contributed) {

        AuditEvent event = new AuditEvent();

        // The header wins; a method can supply the customer when the call did
        // not arrive with one, such as a login that resolves it mid-flight.
        String customerId = RequestContext.customerId() != null
                ? RequestContext.customerId()
                : contributed == null ? null : contributed.getCustomerId();

        // SYSTEM covers scheduled jobs and internal service-to-service calls,
        // which have no customer acting behind them.
        event.setActorId(orDefault(customerId, ActorType.SYSTEM.name()));
        event.setActorType(customerId == null
                ? ActorType.SYSTEM.name()
                : ActorType.CUSTOMER.name());

        event.setAction(annotation.action());
        event.setModule(annotation.module());
        event.setEntity(annotation.entity().isBlank()
                ? annotation.module()
                : annotation.entity());
        event.setDescription(annotation.action() + (failed ? " failed" : " completed"));

        event.setStatusCode(statusCode);
        event.setResponseMessage(failed ? failure.getMessage() : null);
        event.setDurationMs(durationMs);

        HttpServletRequest request = currentRequest();

        if (request != null) {
            event.setApiEndpoint(request.getRequestURI());
            event.setApiMethod(request.getMethod());
        }

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

        commonLogger.audit(event);
    }

    /**
     * The servlet request for this call, when there is one. A scheduled job or
     * an async worker has none, and that is not an error.
     */
    private static HttpServletRequest currentRequest() {

        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        return attributes instanceof ServletRequestAttributes servlet
                ? servlet.getRequest()
                : null;
    }

    /**
     * A controller returning ResponseEntity tells us the status directly. For
     * anything else the true status is not settled until the response is
     * written, well after this aspect returns - so rather than guess, we record
     * nothing and let the request-level interceptor report the real one.
     */
    private static Integer statusCodeOf(Object result) {
        return result instanceof ResponseEntity<?> response
                ? response.getStatusCode().value()
                : null;
    }

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

    /**
     * Converts a domain object into a map so the masker can see inside it.
     *
     * <p>This matters more than it looks. The masker works on keys, so a
     * TransferRequest left as an object would be written via toString() with
     * its OTP field intact. As a map, "otp" is a key the masker recognises and
     * redacts.
     */
    private Object asLoggable(Object value) {

        if (value == null || isSimple(value)) {
            return value;
        }

        try {
            return objectMapper.convertValue(value, Map.class);
        } catch (Exception notConvertible) {
            // Deliberately the class name and not toString(): an object we
            // cannot inspect is an object we cannot mask.
            return value.getClass().getSimpleName();
        }
    }

    private static boolean isSimple(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>;
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
