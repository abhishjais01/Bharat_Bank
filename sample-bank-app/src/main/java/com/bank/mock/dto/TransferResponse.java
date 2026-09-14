package com.bank.mock.dto;

import java.math.BigDecimal;

// result of a transfer
public record TransferResponse(String reference,
                               String cbsReference,
                               String status,
                               BigDecimal amount,
                               String currency,
                               String message) {
}
