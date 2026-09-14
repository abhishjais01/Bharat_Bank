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

// fake core banking system for local testing; ?simulate=... forces failures
@RestController
@RequestMapping("/mock-cbs")
public class MockCbsController {

    private static final String ACCOUNT_WITH_FUNDS = "918273645510";
    private static final BigDecimal BALANCE = new BigDecimal("82450.00");

    // balance
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

    // customer accounts
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

    // transactions
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

    // debit
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

    // CBS_DOWN returns 503, SLOW adds a delay
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

    // true when CBS_DOWN is requested
    private static boolean isDown(String simulate) {
        return "CBS_DOWN".equals(simulate);
    }

    // short delay to simulate a slow CBS
    private static void sleep() {
        try {
            Thread.sleep(700);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
