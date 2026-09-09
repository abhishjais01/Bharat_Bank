package com.bank.mock.dto;

import java.time.Instant;

/** US-09: the cooling-off window is configurable per the bank risk policy. */
public record BeneficiaryResponse(String beneficiaryId,
                                  String name,
                                  String maskedAccountNumber,
                                  String status,
                                  Instant activeFrom) {
}
