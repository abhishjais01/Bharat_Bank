package com.bank.mock.exception;

/** US-10: the beneficiary is unknown, or still inside its cooling-off period. */
public class InvalidBeneficiaryException extends RuntimeException {

    public InvalidBeneficiaryException(String message) {
        super(message);
    }
}
