package com.npst.observability.aop;

import com.npst.observability.contract.LogLevel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a banking operation should be logged, and what to call it.
 *
 * <pre>
 * &#64;LogRegistry(action = "FUND_TRANSFER", module = "PAYMENTS", audit = true)
 * public TransferResponse transfer(TransferRequest request) { ... }
 * </pre>
 *
 * <p>The annotation carries the <b>constant</b> half of the record - the things
 * that are true of the method no matter who calls it. The aspect supplies the
 * <b>runtime</b> half: duration, outcome, status code, response message, the
 * exception if one escaped, and the request context captured at the edge.
 *
 * <p>The value is what disappears from the method body. No trace id plumbing,
 * no bank code, no timing code, no try/catch that exists only to log - the
 * method goes back to expressing the banking operation and nothing else.
 *
 * <p><b>Caveat worth knowing:</b> Spring AOP is proxy based, so a call from one
 * method of a bean to another method <em>on the same bean</em> does not pass
 * through the proxy and will not be intercepted. Annotate the method that is
 * called from outside.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogRegistry {

    /** What the operation does, e.g. FUND_TRANSFER, BALANCE_ENQUIRY. */
    String action();

    /** Which banking domain owns it, e.g. PAYMENTS, ACCOUNTS, CARDS. */
    String module();

    /** The record type acted upon, e.g. TRANSFER, BENEFICIARY. Optional. */
    String entity() default "";

    /** Severity for a successful run. */
    LogLevel level() default LogLevel.INFO;

    /**
     * Exceptions that mean "the request was refused", not "the system broke".
     *
     * <p>A failure is logged at ERROR by default. But insufficient funds, a
     * breached transaction limit or an unknown beneficiary are the system
     * working correctly - they belong at WARN. Logging them as errors trains
     * everyone to ignore the error dashboard, which is how a real outage gets
     * missed.
     */
    Class<? extends Throwable>[] warnOn() default {};

    /**
     * Also write an audit record - who did what to which entity.
     *
     * <p>Reserve this for actions a regulator would ask about: money movement,
     * beneficiary changes, credential resets. A balance enquiry is an
     * application log, not an audit event.
     */
    boolean audit() default false;

    /**
     * Include the method arguments in the log metadata.
     *
     * <p>Off by default. Arguments are converted to a map and run through the
     * masker before they are written, so an OTP inside a request object is
     * redacted rather than printed - but the safest argument is still the one
     * that is never logged.
     */
    boolean logArguments() default false;

    /** Include the returned value, masked the same way. Off by default. */
    boolean logResult() default false;
}
