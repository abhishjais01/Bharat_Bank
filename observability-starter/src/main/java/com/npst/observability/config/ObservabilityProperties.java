package com.npst.observability.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

// all observability.* settings in one place
@ConfigurationProperties(prefix = "observability")
public class ObservabilityProperties {

    private boolean enabled = true;

    private String environment = "DEV";

    private String service;

    // groups of related settings
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

    // which bank this deployment belongs to
    public static class Bank {

        private String code;

        private String name;

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

    // where logs and audit records are sent
    public static class Sink {

        private boolean enabled = true;

        private String endpoint;

        private String auditEndpoint;

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

    // background queue settings
    public static class Async {

        private boolean enabled = true;

        private int queueCapacity = 10_000;

        private int workers = 1;

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

    // trace id header and MDC key
    public static class Trace {

        private String header = "X-Trace-Id";

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

    // request headers that describe the caller
    public static class Context {

        private String channelHeader = "X-Channel";

        private String deviceHeader = "X-Device-Id";

        private String customerHeader = "X-Customer-Id";

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

    // turns @LogRegistry on or off
    public static class Aop {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    // which keys are hidden or partly masked
    public static class Masking {

        private boolean enabled = true;

        private String placeholder = "***REDACTED***";

        // values replaced completely
        private List<String> redactKeys = List.of(
                "otp", "mpin", "tpin", "pin", "atmpin", "cardpin",
                "password", "passwd", "pwd", "newpassword", "oldpassword",
                "token", "accesstoken", "refreshtoken", "idtoken",
                "authorization", "secret", "clientsecret", "apikey",
                "cvv", "cvv2", "biometric", "devicesignature");

        // values partly masked, last few characters kept
        private List<String> maskKeys = List.of(
                "accountnumber", "accountno", "account", "beneficiaryaccount",
                "debitaccount", "creditaccount", "fromaccount", "toaccount",
                "cardnumber", "cardno", "card",
                "pan", "aadhaar", "aadhar",
                "mobile", "mobilenumber", "phone", "phonenumber",
                "email", "emailid",
                "name", "fullname", "firstname", "lastname", "customername",
                "beneficiaryname", "accountholdername", "nomineename");

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
