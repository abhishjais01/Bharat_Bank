package com.npst.observability.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Every knob the observability platform exposes, under one prefix.
 *
 * <p>Replaces the three competing config models this project used to have:
 * {@code observability.*}, {@code observability.bank.*}, and an endpoint read
 * through a raw {@code @Value}. Nothing bound {@code service} at all.
 *
 * <p>A banking microservice adopting the starter needs three lines:
 * <pre>
 * observability:
 *   bank:
 *     code: NPST
 *   environment: DEV
 *   sink:
 *     endpoint: http://logging-api:8090/api/v1/logs
 * </pre>
 * {@code service} is deliberately absent - it defaults to
 * {@code spring.application.name}.
 */
@ConfigurationProperties(prefix = "observability")
public class ObservabilityProperties {

    /** Master switch. When false the starter contributes no beans at all. */
    private boolean enabled = true;

    /** Deployment environment stamped on every log: DEV, UAT, PROD. */
    private String environment = "DEV";

    /** Emitting service name. Falls back to spring.application.name. */
    private String service;

    private final Bank bank = new Bank();
    private final Sink sink = new Sink();
    private final Trace trace = new Trace();
    private final Masking masking = new Masking();
    private final Context context = new Context();
    private final Aop aop = new Aop();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Bank getBank() {
        return bank;
    }

    public Sink getSink() {
        return sink;
    }

    public Trace getTrace() {
        return trace;
    }

    public Masking getMasking() {
        return masking;
    }

    public Context getContext() {
        return context;
    }

    public Aop getAop() {
        return aop;
    }

    /** Identity of the institution this deployment belongs to. */
    public static class Bank {

        /** Short code stamped on every log line, e.g. NPST. */
        private String code;

        /** Human readable name, for dashboards and reports. */
        private String name;

        /** Deployment region, e.g. IN. */
        private String region;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }

    /** Where structured logs are shipped for central storage. */
    public static class Sink {

        /**
         * Turn off to keep file/Loki logging while sending nothing over HTTP.
         * logging-api itself sets this false so it cannot log to itself.
         */
        private boolean enabled = true;

        /** Absolute URL of the logging-api application log endpoint. */
        private String endpoint;

        /**
         * Absolute URL of the audit endpoint. Separate because audit lands in a
         * different table with different retention and different grants.
         */
        private String auditEndpoint;

        /**
         * Deliberately short. Logging must never become the reason a customer
         * request hangs - the original RestTemplate had no timeout at all, so a
         * stalled logging-api would block bank threads indefinitely.
         */
        private Duration connectTimeout = Duration.ofMillis(500);

        private Duration readTimeout = Duration.ofSeconds(1);

        private final Async async = new Async();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAuditEndpoint() {
            return auditEndpoint;
        }

        public void setAuditEndpoint(String auditEndpoint) {
            this.auditEndpoint = auditEndpoint;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public Async getAsync() {
            return async;
        }
    }

    /**
     * Hands the log to a background worker so the customer's request thread
     * never waits on the observability platform.
     */
    public static class Async {

        /** Set false to ship synchronously - useful in tests. */
        private boolean enabled = true;

        /**
         * Bounded on purpose. An unbounded queue turns a logging-api outage
         * into an OutOfMemoryError in the banking service.
         */
        private int queueCapacity = 10_000;

        /** Worker threads draining the queue. One is enough for HTTP. */
        private int workers = 1;

        /** How long shutdown waits for the queue to drain. */
        private Duration shutdownTimeout = Duration.ofSeconds(5);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public int getWorkers() {
            return workers;
        }

        public void setWorkers(int workers) {
            this.workers = workers;
        }

        public Duration getShutdownTimeout() {
            return shutdownTimeout;
        }

        public void setShutdownTimeout(Duration shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
        }
    }

    /** Correlation id propagation. */
    public static class Trace {

        /**
         * Header carrying the correlation id. The gateway generates it; each
         * service honours it and only creates one when it is absent.
         */
        private String header = "X-Trace-Id";

        /** MDC key the id is published under, for logback patterns. */
        private String mdcKey = "traceId";

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public String getMdcKey() {
            return mdcKey;
        }

        public void setMdcKey(String mdcKey) {
            this.mdcKey = mdcKey;
        }
    }

    /**
     * Headers that tell us where a request came from.
     *
     * <p>The gateway and the mobile app set these; every value is optional, so
     * a scheduled job or an internal call simply carries fewer of them.
     */
    public static class Context {

        /** MOBILE, WEB, BRANCH, ATM, API. */
        private String channelHeader = "X-Channel";

        /** Device fingerprint, for tying a session to a handset. */
        private String deviceHeader = "X-Device-Id";

        /** CIF or customer reference of the person the request acts for. */
        private String customerHeader = "X-Customer-Id";

        /**
         * Checked in order for the caller's real address. Behind a load
         * balancer the socket address is the balancer, not the customer, so a
         * forwarded header has to win when present.
         */
        private List<String> ipHeaders = List.of("X-Forwarded-For", "X-Real-IP");

        public String getChannelHeader() {
            return channelHeader;
        }

        public void setChannelHeader(String channelHeader) {
            this.channelHeader = channelHeader;
        }

        public String getDeviceHeader() {
            return deviceHeader;
        }

        public void setDeviceHeader(String deviceHeader) {
            this.deviceHeader = deviceHeader;
        }

        public String getCustomerHeader() {
            return customerHeader;
        }

        public void setCustomerHeader(String customerHeader) {
            this.customerHeader = customerHeader;
        }

        public List<String> getIpHeaders() {
            return ipHeaders;
        }

        public void setIpHeaders(List<String> ipHeaders) {
            this.ipHeaders = ipHeaders;
        }
    }

    /** The @LogRegistry aspect. */
    public static class Aop {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Stops sensitive banking data reaching the log file, Loki, or MySQL.
     *
     * <p>The PRD's Security NFR forbids OTPs, tokens and biometric data in
     * plaintext logs, and metadata is a free-form map - so without this a
     * developer can put an OTP into a log with no review catching it.
     *
     * <p>Matching is on the normalised key (lower-cased, {@code _} and
     * {@code -} removed) and is exact. Predictable beats clever: substring
     * matching would have "pin" swallow "shipping".
     */
    public static class Masking {

        private boolean enabled = true;

        /** What a redacted value is replaced with. */
        private String placeholder = "***REDACTED***";

        /**
         * Values removed entirely. These have no diagnostic value and real
         * regulatory consequences if they leak.
         */
        private List<String> redactKeys = List.of(
                "otp", "mpin", "tpin", "pin", "atmpin", "cardpin",
                "password", "passwd", "pwd", "newpassword", "oldpassword",
                "token", "accesstoken", "refreshtoken", "idtoken",
                "authorization", "secret", "clientsecret", "apikey",
                "cvv", "cvv2", "biometric", "devicesignature");

        /**
         * Values partially masked - enough to correlate a support call, not
         * enough to identify or reuse the instrument.
         */
        private List<String> maskKeys = List.of(
                "accountnumber", "accountno", "account", "beneficiaryaccount",
                "cardnumber", "cardno", "card",
                "pan", "aadhaar", "aadhar",
                "mobile", "mobilenumber", "phone", "phonenumber",
                "email", "emailid");

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPlaceholder() {
            return placeholder;
        }

        public void setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
        }

        public List<String> getRedactKeys() {
            return redactKeys;
        }

        public void setRedactKeys(List<String> redactKeys) {
            this.redactKeys = redactKeys;
        }

        public List<String> getMaskKeys() {
            return maskKeys;
        }

        public void setMaskKeys(List<String> maskKeys) {
            this.maskKeys = maskKeys;
        }
    }
}
