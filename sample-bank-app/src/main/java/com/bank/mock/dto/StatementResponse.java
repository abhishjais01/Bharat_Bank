package com.bank.mock.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// statement for a date range
public record StatementResponse(String accountNumber,
                                LocalDate from,
                                LocalDate to,
                                int transactionCount,
                                List<Map<String, Object>> transactions) {
}
