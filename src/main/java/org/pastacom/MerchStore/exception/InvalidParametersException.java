package org.pastacom.MerchStore.exception;

public class InvalidParametersException extends RuntimeException {

    public InvalidParametersException() {
    }

    public InvalidParametersException(String message) {
        super(message);
    }

    public InvalidParametersException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidParametersException(Throwable cause) {
        super(cause);
    }
}