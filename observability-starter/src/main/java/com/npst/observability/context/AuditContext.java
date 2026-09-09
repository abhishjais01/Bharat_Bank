package com.npst.observability.context;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lets a business method contribute the details only it can know.
 *
 * <p>{@code @LogRegistry} supplies the constants and the aspect supplies what
 * it can observe from outside - duration, outcome, status code. Neither can
 * know that this particular transfer moved 5,000 rupees under reference
 * IMPS20260909001. Without a seam like this the amount and business_ref columns
 * would exist and always be null.
 *
 * <pre>
 * &#64;LogRegistry(action = "FUND_TRANSFER", module = "PAYMENTS", audit = true)
 * public TransferResponse transfer(TransferRequest request) {
 *
 *     AuditContext.amount(request.amount(), "INR");
 *     AuditContext.businessRef(reference);
 *     AuditContext.entityId(transactionId);
 *
 *     return ...;
 * }
 * </pre>
 *
 * <p>Thread-local, drained by the aspect when the method returns, and cleared
 * whether the method succeeded or threw - a value left behind would attach
 * itself to the next unrelated transaction on that thread, which in a banking
 * audit trail is worse than having no value at all.
 */
public final class AuditContext {

    private static final ThreadLocal<Details> CURRENT = new ThreadLocal<>();

    private AuditContext() {
    }

    /** Monetary value of the operation, with its ISO 4217 currency. */
    public static void amount(BigDecimal amount, String currency) {
        Details details = current();
        details.amount = amount;
        details.currency = currency;
    }

    /** The reference a customer or the business would quote. */
    public static void businessRef(String businessRef) {
        current().businessRef = businessRef;
    }

    /** Identifier of the record acted upon. */
    public static void entityId(String entityId) {
        current().entityId = entityId;
    }

    /** The customer this acted for, when it is not on the request headers. */
    public static void customerId(String customerId) {
        current().customerId = customerId;
    }

    /** Unmasked in the audit table, masked in the log file. */
    public static void mobileNumber(String mobileNumber) {
        current().mobileNumber = mobileNumber;
    }

    /** Any domain-specific field with no column of its own. */
    public static void put(String key, Object value) {
        current().businessContext.put(key, value);
    }

    /** State before the change, for actions that alter a record. */
    public static void beforeState(Map<String, Object> state) {
        current().beforeState = state;
    }

    /** State after the change. */
    public static void afterState(Map<String, Object> state) {
        current().afterState = state;
    }

    /** Whatever the method contributed, or null. Clears as it reads. */
    public static Details drain() {

        Details details = CURRENT.get();
        CURRENT.remove();

        return details;
    }

    /** Discards anything set on this thread. */
    public static void clear() {
        CURRENT.remove();
    }

    private static Details current() {

        Details details = CURRENT.get();

        if (details == null) {
            details = new Details();
            CURRENT.set(details);
        }

        return details;
    }

    /** What a business method contributed for this one call. */
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
