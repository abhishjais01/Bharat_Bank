package com.npst.observability.controller;

import com.npst.observability.aop.LogRegistry;
import com.npst.observability.context.AuditContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Smoke endpoints for the platform.
 *
 * <p>Compare with what this used to be: two explicit logging calls, a metadata
 * map built by hand, and an audit call the controller had no business making.
 * The annotations now carry the constant half of each record and the aspect
 * supplies the rest.
 *
 * <p>Step 7 replaces these with the real journeys from the PRD - balance,
 * summary, statement, beneficiary and IMPS transfer.
 */
@RestController
@RequestMapping("/api/v1")
public class HelloController {

    @GetMapping("/hello")
    @LogRegistry(action = "HELLO", module = "SMOKE")
    public String hello() {
        return "Hello API Success";
    }

    /**
     * A money movement - so audited, unlike the balance enquiries around it.
     *
     * <p>Note what the method does and does not do. It declares the amount and
     * reference through AuditContext, because only it knows them. It never
     * touches the trace id, the bank code, the channel, the device, the caller
     * IP, the duration or the outcome; all of that is attached for it.
     */
    @PostMapping("/transfers/imps")
    @LogRegistry(action = "FUND_TRANSFER", module = "PAYMENTS",
            entity = "TRANSFER", audit = true, logArguments = true)
    public Map<String, Object> transfer(@RequestBody Map<String, Object> request) {

        String reference = "IMPS" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BigDecimal amount = new BigDecimal(String.valueOf(request.getOrDefault("amount", "0")));

        AuditContext.amount(amount, "INR");
        AuditContext.businessRef(reference);
        AuditContext.entityId(reference);
        AuditContext.put("transferType", "IMPS");

        return Map.of(
                "status", "SUCCESS",
                "reference", reference,
                "amount", amount);
    }
}
