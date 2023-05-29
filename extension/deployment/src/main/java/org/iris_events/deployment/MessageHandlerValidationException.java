package org.iris_events.deployment;

public class MessageHandlerValidationException extends RuntimeException {
    public MessageHandlerValidationException() {
    }

    public MessageHandlerValidationException(String message) {
        super(message);
    }

    public MessageHandlerValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageHandlerValidationException(Throwable cause) {
        super(cause);
    }

    public MessageHandlerValidationException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
