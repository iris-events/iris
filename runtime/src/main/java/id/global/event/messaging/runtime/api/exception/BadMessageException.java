package id.global.event.messaging.runtime.api.exception;

import id.global.event.messaging.runtime.api.error.MessagingError;

/**
 * Amqp bad message exception indicating client error.
 * Iris EDA will return the message to the client session immediately.
 */
public class BadMessageException extends MessagingException {

    public BadMessageException(final MessagingError messageError, final String message) {
        super(messageError, message);
    }

    public BadMessageException(final MessagingError messageError, final String message, final Throwable cause) {
        super(messageError, message, cause);
    }
}
