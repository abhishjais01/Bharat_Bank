package com.bank.mock.cbs;

/**
 * The core banking system could not be reached, or answered with a fault.
 *
 * <p>A genuine system failure, so it is logged at ERROR - unlike the business
 * rejections in the exception package, which are the system working correctly.
 */
public class CbsUnavailableException extends RuntimeException {

    public CbsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
