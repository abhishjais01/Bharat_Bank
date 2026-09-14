package com.bank.mock.cbs;

// CBS could not be reached or returned an error; logged as ERROR
public class CbsUnavailableException extends RuntimeException {

    public CbsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
