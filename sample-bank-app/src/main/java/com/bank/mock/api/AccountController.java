package com.bank.mock.api;

import com.bank.mock.cbs.CbsClient;
import com.bank.mock.dto.AccountSummaryResponse;
import com.bank.mock.dto.BalanceResponse;
import com.bank.mock.dto.StatementResponse;
import com.bank.mock.exception.InvalidStatementPeriodException;
import com.npst.observability.aop.LogRegistry;
import com.npst.observability.context.RequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

// account endpoints: balance, summary, statement
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final CbsClient cbs;
    private final int maxStatementDays;

    public AccountController(CbsClient cbs,
                             @Value("${mock.statement.max-days:365}") int maxStatementDays) {
        this.cbs = cbs;
        this.maxStatementDays = maxStatementDays;
    }

    // balance enquiry; logged but not audited
    @GetMapping("/{accountNumber}/balance")
    @LogRegistry(action = "BALANCE_ENQUIRY", module = "ACCOUNTS", entity = "ACCOUNT")
    public ResponseEntity<BalanceResponse> balance(
            @PathVariable String accountNumber,
            @RequestParam(required = false) String simulate) {

        // ask the core banking system
        BigDecimal balance = cbs.fetchBalance(accountNumber, simulate);

        return ResponseEntity.ok(new BalanceResponse(accountNumber, balance, "INR"));
    }

    // all accounts of the customer, one CBS call per account
    @GetMapping("/summary")
    @LogRegistry(action = "ACCOUNT_SUMMARY", module = "ACCOUNTS", entity = "CUSTOMER")
    public ResponseEntity<AccountSummaryResponse> summary(
            @RequestParam(required = false) String simulate) {

        // customer id comes from the request header
        String customerId = RequestContext.customerId();

        List<Map<String, Object>> accounts = cbs.fetchAccounts(customerId, simulate);

        accounts.forEach(account ->
                cbs.fetchBalance(String.valueOf(account.get("accountNumber")), null));

        return ResponseEntity.ok(
                new AccountSummaryResponse(customerId, accounts.size(), accounts));
    }

    // statement for a date range; a too-long range is logged as WARN
    @GetMapping("/{accountNumber}/statement")
    @LogRegistry(action = "STATEMENT_DOWNLOAD", module = "ACCOUNTS", entity = "ACCOUNT",
            warnOn = InvalidStatementPeriodException.class)
    public ResponseEntity<StatementResponse> statement(
            @PathVariable String accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String simulate) {

        // reject ranges longer than the allowed maximum
        long days = ChronoUnit.DAYS.between(from, to);

        if (days > maxStatementDays) {
            throw new InvalidStatementPeriodException(
                    "Statement period of " + days + " days exceeds the "
                            + maxStatementDays + " day maximum");
        }

        // fetch the transactions from CBS
        List<Map<String, Object>> transactions =
                cbs.fetchTransactions(accountNumber, from, to, simulate);

        return ResponseEntity.ok(new StatementResponse(
                accountNumber, from, to, transactions.size(), transactions));
    }
}
