package com.bank.mock.exception;

// statement date range is too long
public class InvalidStatementPeriodException extends RuntimeException {

    public InvalidStatementPeriodException(String message) {
        super(message);
    }
}
