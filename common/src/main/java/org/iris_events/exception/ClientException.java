package org.iris_events.exception;

import org.iris_events.common.ErrorType;

public abstract class ClientException extends MessagingException {
    public ClientException(final ErrorType errorType, final String clientCode, final String message) {
        super(errorType, clientCode, message);
    }

    public ClientException(final ErrorType errorType, final String clientCode, final String message, final Throwable cause) {
        super(errorType, clientCode, message, cause);
    }
}
