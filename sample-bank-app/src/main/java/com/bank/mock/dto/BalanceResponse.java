package com.bank.mock.dto;

import java.math.BigDecimal;

/** US-07: the balance a customer sees after tapping Check Balance. */
public record BalanceResponse(String accountNumber, BigDecimal balance, String currency) {
}
