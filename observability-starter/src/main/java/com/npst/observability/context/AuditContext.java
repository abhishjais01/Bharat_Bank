package com.npst.observability.context;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

// lets a controller add audit details (amount, reference, state) for the current call
public final class AuditContext {

    private static final ThreadLocal<Details> CURRENT = new ThreadLocal<>();

    private AuditContext() {
    }

    // setters used inside the business method
    public static void amount(BigDecimal amount, String currency) {
        Details details = current();
        details.amount = amount;
        details.currency = currency;
    }

    public static void businessRef(String businessRef) {
        current().businessRef = businessRef;
    }

    public static void entityId(String entityId) {
        current().entityId = entityId;
    }

    public static void customerId(String customerId) {
        current().customerId = customerId;
    }

    public static void mobileNumber(String mobileNumber) {
        current().mobileNumber = mobileNumber;
    }

    public static void put(String key, Object value) {
        current().businessContext.put(key, value);
    }

    public static void beforeState(Map<String, Object> state) {
        current().beforeState = state;
    }

    public static void afterState(Map<String, Object> state) {
        current().afterState = state;
    }

    // read and clear; called by the aspect when the method finishes
    public static Details drain() {

        Details details = CURRENT.get();
        CURRENT.remove();

        return details;
    }

    // clear without reading
    public static void clear() {
        CURRENT.remove();
    }

    // details for this thread, created on first use
    private static Details current() {

        Details details = CURRENT.get();

        if (details == null) {
            details = new Details();
            CURRENT.set(details);
        }

        return details;
    }

    // values collected for one call
    public static final class Details {

        private BigDecimal amount;
        private String currency;
        private String businessRef;
        private String entityId;
        private String customerId;
        private String mobileNumber;
        private Map<String, Object> beforeState;
        private Map<String, Object> afterState;
        private final Map<String, Object> businessContext = new LinkedHashMap<>();

        public BigDecimal getAmount() {
            return amount;
        }

        public String getCurrency() {
            return currency;
        }

        public String getBusinessRef() {
            return businessRef;
        }

        public String getEntityId() {
            return entityId;
        }

        public String getCustomerId() {
            return customerId;
        }

        public String getMobileNumber() {
            return mobileNumber;
        }

        public Map<String, Object> getBeforeState() {
            return beforeState;
        }

        public Map<String, Object> getAfterState() {
            return afterState;
        }

        public Map<String, Object> getBusinessContext() {
            return businessContext.isEmpty() ? null : businessContext;
        }
    }
}
