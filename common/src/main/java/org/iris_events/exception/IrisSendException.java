package org.iris_events.exception;

public class IrisSendException extends RuntimeException {
    public IrisSendException(String message) {
        super(message);
    }

    public IrisSendException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
