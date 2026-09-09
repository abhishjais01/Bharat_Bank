package com.bank.mock.dto;

import java.math.BigDecimal;

/**
 * US-10: status is shown immediately and reconciled asynchronously when
 * PENDING, so the customer is never left on a spinner.
 */
public record TransferResponse(String reference,
                               String cbsReference,
                               String status,
                               BigDecimal amount,
                               String currency,
                               String message) {
}
