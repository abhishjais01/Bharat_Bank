package com.bank.mock.dto;

import java.util.List;
import java.util.Map;

/** US-07: every account the customer holds, in one view. */
public record AccountSummaryResponse(String customerId,
                                     int accountCount,
                                     List<Map<String, Object>> accounts) {
}
