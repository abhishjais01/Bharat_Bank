package com.bank.mock.exception;

// not enough balance for the transfer
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
