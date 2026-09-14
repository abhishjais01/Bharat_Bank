package com.bank.mock.dto;

import jakarta.validation.constraints.NotBlank;

// request to add a beneficiary (includes an OTP)
public record BeneficiaryRequest(
        @NotBlank String name,
        @NotBlank String accountNumber,
        @NotBlank String ifsc,
        String mobile,
        @NotBlank String otp) {
}
