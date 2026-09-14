package com.bank.mock.dto;

import java.util.List;
import java.util.Map;

// all accounts of a customer
public record AccountSummaryResponse(String customerId,
                                     int accountCount,
                                     List<Map<String, Object>> accounts) {
}
