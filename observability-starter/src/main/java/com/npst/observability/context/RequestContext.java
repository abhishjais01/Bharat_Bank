package com.npst.observability.context;

import org.slf4j.MDC;

// caller info for this request (trace id, channel, device, IP, customer), stored in MDC
public final class RequestContext {

    public static final String TRACE_ID = "traceId";
    public static final String CHANNEL = "channel";
    public static final String DEVICE_ID = "deviceId";
    public static final String IP_ADDRESS = "ipAddress";
    public static final String CUSTOMER_ID = "customerId";

    public static final String[] KEYS = {
            TRACE_ID, CHANNEL, DEVICE_ID, IP_ADDRESS, CUSTOMER_ID
    };

    private RequestContext() {
    }

    // getters, null when not set
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

    // store a value, skipping empty ones
    public static void put(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }

    // remove only our keys from MDC
    public static void clear() {
        for (String key : KEYS) {
            MDC.remove(key);
        }
    }
}
