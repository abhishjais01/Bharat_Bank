package com.bank.mock.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * US-09: adding a beneficiary requires OTP confirmation.
 *
 * <p>The OTP field is the point of this endpoint as far as the observability
 * platform is concerned. It must never reach the log file, Loki or MySQL, and
 * the masker is what stops it.
 */
public record BeneficiaryRequest(
        @NotBlank String name,
        @NotBlank String accountNumber,
        @NotBlank String ifsc,
        String mobile,
        @NotBlank String otp) {
}
