package com.bank.mock.exception;

/** US-10: the transfer is above the customer's per-transaction limit. */
public class LimitExceededException extends RuntimeException {

    public LimitExceededException(String message) {
        super(message);
    }
}
