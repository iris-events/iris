package org.iris_events.exception;

public class IrisConnectionException extends RuntimeException {
    public IrisConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public IrisConnectionException(String message) {
        super(message);
    }
}
