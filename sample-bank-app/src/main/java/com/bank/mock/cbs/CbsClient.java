package com.bank.mock.cbs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// calls to the core banking system
public interface CbsClient {

    BigDecimal fetchBalance(String accountNumber, String simulate);

    List<Map<String, Object>> fetchAccounts(String customerId, String simulate);

    List<Map<String, Object>> fetchTransactions(String accountNumber,
                                                LocalDate from,
                                                LocalDate to,
                                                String simulate);

    String debit(String accountNumber, BigDecimal amount, String simulate);
}
