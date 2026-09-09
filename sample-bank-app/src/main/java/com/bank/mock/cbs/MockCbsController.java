package com.bank.mock.cbs;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A stand-in Core Banking System.
 *
 * <p>Lives inside this application but is reached over real HTTP, which is the
 * whole reason it exists here rather than as an in-memory stub: a correlation
 * id that never crosses a network boundary has not actually been proven to
 * propagate. The inbound request context filter sees the forwarded
 * {@code X-Trace-Id} and reuses it, so a balance enquiry and the CBS call it
 * makes share one trace.
 *
 * <p>When the team's Swagger mock CBS is ready this class is deleted and
 * {@code mock.cbs.base-url} points at it instead.
 */
@RestController
@RequestMapping("/mock-cbs")
public class MockCbsController {

    private static final String ACCOUNT_WITH_FUNDS = "918273645510";
    private static final BigDecimal BALANCE = new BigDecimal("82450.00");

    @GetMapping("/accounts/{accountNumber}/balance")
    public ResponseEntity<Map<String, Object>> balance(
            @PathVariable String accountNumber,
            @RequestParam(required = false) String simulate) {

        ResponseEntity<Map<String, Object>> failure = simulatedFailure(simulate);

        if (failure != null) {
            return failure;
        }

        return ResponseEntity.ok(Map.of(
                "accountNumber", accountNumber,
                "balance", ACCOUNT_WITH_FUNDS.equals(accountNumber) ? BALANCE : new BigDecimal("1250.00"),
                "currency", "INR"));
    }

    /** US-07: every account type the customer holds. */
    @GetMapping("/customers/{customerId}/accounts")
    public ResponseEntity<List<Map<String, Object>>> accounts(
            @PathVariable String customerId,
            @RequestParam(required = false) String simulate) {

        if (isDown(simulate)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        return ResponseEntity.ok(List.of(
                Map.of("accountNumber", ACCOUNT_WITH_FUNDS, "type", "SAVINGS",
                        "balance", BALANCE, "currency", "INR"),
                Map.of("accountNumber", "918273645511", "type", "CURRENT",
                        "balance", new BigDecimal("240310.75"), "currency", "INR"),
                Map.of("accountNumber", "918273645512", "type", "TERM_DEPOSIT",
                        "balance", new BigDecimal("500000.00"), "currency", "INR"),
                Map.of("accountNumber", "918273645513", "type", "LOAN",
                        "balance", new BigDecimal("-1875000.00"), "currency", "INR")));
    }

    /**
     * US-08. A period with no activity returns an empty list, not an error -
     * the PRD is explicit that a zero-transaction statement is a valid result.
     */
    @GetMapping("/accounts/{accountNumber}/transactions")
    public ResponseEntity<List<Map<String, Object>>> transactions(
            @PathVariable String accountNumber,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String simulate) {

        if (isDown(simulate)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        if ("EMPTY_PERIOD".equals(simulate)) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(List.of(
                Map.of("date", from, "description", "UPI/ZOMATO", "debit", new BigDecimal("480.00")),
                Map.of("date", from, "description", "SALARY CREDIT", "credit", new BigDecimal("95000.00")),
                Map.of("date", to, "description", "IMPS/RENT", "debit", new BigDecimal("28000.00"))));
    }

    /** The only call that moves money. */
    @PostMapping("/transfers")
    public ResponseEntity<Map<String, Object>> debit(@RequestBody Map<String, Object> request,
                                                     @RequestParam(required = false) String simulate) {

        if (isDown(simulate)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "CBS is not responding"));
        }

        if ("INSUFFICIENT_FUNDS".equals(simulate)) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(Map.of("error", "Insufficient balance in the debit account"));
        }

        return ResponseEntity.ok(Map.of(
                "cbsReference", "CBS" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(),
                "status", "DEBITED",
                "amount", request.getOrDefault("amount", "0")));
    }

    private static ResponseEntity<Map<String, Object>> simulatedFailure(String simulate) {

        if (isDown(simulate)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "CBS is not responding"));
        }

        if ("SLOW".equals(simulate)) {
            sleep();
        }

        return null;
    }

    private static boolean isDown(String simulate) {
        return "CBS_DOWN".equals(simulate);
    }

    /** Long enough to be visible in durationMs, short enough not to time out. */
    private static void sleep() {
        try {
            Thread.sleep(700);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
