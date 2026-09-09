package com.bank.mock.exception;

/** US-08: the requested statement range is wider than policy allows. */
public class InvalidStatementPeriodException extends RuntimeException {

    public InvalidStatementPeriodException(String message) {
        super(message);
    }
}
