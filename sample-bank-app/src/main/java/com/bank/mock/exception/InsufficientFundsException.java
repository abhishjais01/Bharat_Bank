package com.bank.mock.exception;

/** US-10: the debit account cannot cover the transfer. */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
