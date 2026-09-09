package com.bank.mock.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * US-08. A period with no activity is a valid statement, not an error - the
 * PRD calls for an empty-state result rather than a failure.
 */
public record StatementResponse(String accountNumber,
                                LocalDate from,
                                LocalDate to,
                                int transactionCount,
                                List<Map<String, Object>> transactions) {
}
