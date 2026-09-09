package com.npst.observability.context;

import org.slf4j.MDC;

/**
 * The "where did this request come from" block, carried in MDC for the life of
 * the request.
 *
 * <p>MDC rather than a request-scoped bean on purpose: every log line the
 * service writes - including plain SLF4J calls from code that knows nothing
 * about this platform - can pick these values up from the logging pattern, and
 * the SDK reads the same source. One place, no duplication.
 *
 * <p>All values are optional. A scheduled job or an internal service-to-service
 * call has no channel or device, and that is not an error.
 */
public final class RequestContext {

    public static final String TRACE_ID = "traceId";
    public static final String CHANNEL = "channel";
    public static final String DEVICE_ID = "deviceId";
    public static final String IP_ADDRESS = "ipAddress";
    public static final String CUSTOMER_ID = "customerId";

    /** Every key this class owns, so a filter can clear exactly its own. */
    public static final String[] KEYS = {
            TRACE_ID, CHANNEL, DEVICE_ID, IP_ADDRESS, CUSTOMER_ID
    };

    private RequestContext() {
    }

    public static String traceId() {
        return MDC.get(TRACE_ID);
    }

    public static String channel() {
        return MDC.get(CHANNEL);
    }

    public static String deviceId() {
        return MDC.get(DEVICE_ID);
    }

    public static String ipAddress() {
        return MDC.get(IP_ADDRESS);
    }

    public static String customerId() {
        return MDC.get(CUSTOMER_ID);
    }

    /** Ignores null and blank so MDC never holds empty strings. */
    public static void put(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }

    /**
     * Removes only this platform's keys. {@code MDC.clear()} would also wipe
     * whatever the host application put there for its own logging.
     */
    public static void clear() {
        for (String key : KEYS) {
            MDC.remove(key);
        }
    }
}
