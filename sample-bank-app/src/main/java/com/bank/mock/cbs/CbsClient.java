package com.bank.mock.cbs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The bank's window onto the Core Banking System.
 *
 * <p>An interface because the CBS behind it is going to change. Today it is
 * {@link HttpCbsClient} talking to the stub endpoints in this same application;
 * when the team's Swagger mock CBS is ready it becomes a base URL change, and
 * when the real CBS arrives it becomes another implementation. Nothing above
 * this line moves.
 *
 * <p>{@code simulate} exists only in the mock and lets a caller ask for a
 * specific failure - CBS_DOWN, INSUFFICIENT_FUNDS - so the logging platform can
 * be exercised against outcomes that are otherwise hard to produce on demand.
 */
public interface CbsClient {

    BigDecimal fetchBalance(String accountNumber, String simulate);

    List<Map<String, Object>> fetchAccounts(String customerId, String simulate);

    List<Map<String, Object>> fetchTransactions(String accountNumber,
                                                LocalDate from,
                                                LocalDate to,
                                                String simulate);

    /** Debits the account and returns the CBS reference for the movement. */
    String debit(String accountNumber, BigDecimal amount, String simulate);
}
