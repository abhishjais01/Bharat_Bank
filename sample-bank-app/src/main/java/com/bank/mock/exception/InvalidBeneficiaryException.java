package com.bank.mock.exception;

// unknown beneficiary, or still in the cooling-off period
public class InvalidBeneficiaryException extends RuntimeException {

    public InvalidBeneficiaryException(String message) {
        super(message);
    }
}
