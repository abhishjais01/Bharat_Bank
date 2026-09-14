package com.npst.observability.aop;

import com.npst.observability.contract.LogLevel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// put this on a controller method to log it automatically (and audit it when audit = true)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogRegistry {

    // name of the operation, e.g. FUND_TRANSFER
    String action();

    // business area, e.g. PAYMENTS
    String module();

    // type of record the operation works on, e.g. TRANSFER
    String entity() default "";

    // level used when the method succeeds
    LogLevel level() default LogLevel.INFO;

    // expected business errors, logged as WARN instead of ERROR
    Class<? extends Throwable>[] warnOn() default {};

    // also write an audit record
    boolean audit() default false;

    // add the method arguments (masked) to the log
    boolean logArguments() default false;

    // add the return value (masked) to the log
    boolean logResult() default false;
}
