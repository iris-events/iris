package id.global.event.messaging.runtime.api.exception;

import id.global.event.messaging.runtime.api.error.IMessagingError;

/**
 * Amqp bad message exception indicating client error.
 * Iris EDA will return the message to the client session immediately.
 */
public class BadMessageException extends MessagingException {

    public BadMessageException(final IMessagingError messageError, final String message) {
        super(messageError, message);
    }

    public BadMessageException(final IMessagingError messageError, final String message, final Throwable cause) {
        super(messageError, message, cause);
    }
}
