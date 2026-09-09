package com.npst.observability.controller;

import com.npst.observability.aop.LogRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Smoke endpoint for the platform.
 *
 * <p>Compare this with what it used to be: two explicit logging calls, a
 * metadata map built by hand, and an audit call the controller had no business
 * making. The annotation now carries the constant half of that record and the
 * aspect supplies the rest, so the method is back to expressing only what it
 * does.
 *
 * <p>Step 7 replaces this with the real journeys from the PRD - balance,
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
}
