package com.bank.mock.dto;

import java.math.BigDecimal;

// account balance
public record BalanceResponse(String accountNumber, BigDecimal balance, String currency) {
}
