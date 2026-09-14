package com.bank.mock.exception;

// amount is above the transfer limit
public class LimitExceededException extends RuntimeException {

    public LimitExceededException(String message) {
        super(message);
    }
}
