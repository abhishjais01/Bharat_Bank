package com.bank.mock.dto;

import java.time.Instant;

// result of adding a beneficiary
public record BeneficiaryResponse(String beneficiaryId,
                                  String name,
                                  String maskedAccountNumber,
                                  String status,
                                  Instant activeFrom) {
}
