package com.bank.mock.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// IMPS transfer request
public record TransferRequest(
        @NotBlank String debitAccount,
        @NotBlank String beneficiaryAccount,
        @NotNull @DecimalMin("1.00") BigDecimal amount,
        String remarks,
        String otp) {
}
