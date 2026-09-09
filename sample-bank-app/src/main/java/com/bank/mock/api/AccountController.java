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

/**
 * Account services from the PRD, section 6.3.
 *
 * <p>Read the handlers and notice what is absent. No trace id, no bank code, no
 * service name, no timing, no try/catch that exists only to log, no masking
 * call. Each method expresses the banking operation; the annotation names it
 * and the platform records the rest.
 */
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

    /**
     * The journey from the brief: customer taps Check Balance, sees Rs 82,450.
     *
     * <p>Not audited. A balance enquiry is an application log - auditing every
     * read is how an audit trail turns into noise nobody can search.
     */
    @GetMapping("/{accountNumber}/balance")
    @LogRegistry(action = "BALANCE_ENQUIRY", module = "ACCOUNTS", entity = "ACCOUNT")
    public ResponseEntity<BalanceResponse> balance(
            @PathVariable String accountNumber,
            @RequestParam(required = false) String simulate) {

        BigDecimal balance = cbs.fetchBalance(accountNumber, simulate);

        return ResponseEntity.ok(new BalanceResponse(accountNumber, balance, "INR"));
    }

    /**
     * US-07: savings, current, deposit and loan in one view.
     *
     * <p>Several CBS calls behind one customer action - which is exactly the
     * shape a correlation id exists for. All of them appear under one trace.
     */
    @GetMapping("/summary")
    @LogRegistry(action = "ACCOUNT_SUMMARY", module = "ACCOUNTS", entity = "CUSTOMER")
    public ResponseEntity<AccountSummaryResponse> summary(
            @RequestParam(required = false) String simulate) {

        String customerId = RequestContext.customerId();

        List<Map<String, Object>> accounts = cbs.fetchAccounts(customerId, simulate);

        // A second hop per account, so the fan-out is real rather than implied.
        accounts.forEach(account ->
                cbs.fetchBalance(String.valueOf(account.get("accountNumber")), null));

        return ResponseEntity.ok(
                new AccountSummaryResponse(customerId, accounts.size(), accounts));
    }

    /**
     * US-08: statement for a chosen period.
     *
     * <p>Two outcomes the PRD is explicit about, and they are logged
     * differently. A period with no transactions is a successful empty
     * statement, logged at INFO. A range wider than policy is a rejected
     * request, logged at WARN through {@code warnOn} - not ERROR, because
     * nothing is broken.
     */
    @GetMapping("/{accountNumber}/statement")
    @LogRegistry(action = "STATEMENT_DOWNLOAD", module = "ACCOUNTS", entity = "ACCOUNT",
            warnOn = InvalidStatementPeriodException.class)
    public ResponseEntity<StatementResponse> statement(
            @PathVariable String accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String simulate) {

        long days = ChronoUnit.DAYS.between(from, to);

        if (days > maxStatementDays) {
            throw new InvalidStatementPeriodException(
                    "Statement period of " + days + " days exceeds the "
                            + maxStatementDays + " day maximum");
        }

        List<Map<String, Object>> transactions =
                cbs.fetchTransactions(accountNumber, from, to, simulate);

        return ResponseEntity.ok(new StatementResponse(
                accountNumber, from, to, transactions.size(), transactions));
    }
}
