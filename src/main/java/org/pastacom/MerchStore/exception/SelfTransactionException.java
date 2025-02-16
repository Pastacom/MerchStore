package org.pastacom.MerchStore.exception;

public class SelfTransactionException extends RuntimeException {

    public SelfTransactionException() {
    }

    public SelfTransactionException(String message) {
        super(message);
    }

    public SelfTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public SelfTransactionException(Throwable cause) {
        super(cause);
    }
}
