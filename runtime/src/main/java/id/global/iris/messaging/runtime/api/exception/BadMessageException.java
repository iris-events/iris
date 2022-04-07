package id.global.iris.messaging.runtime.api.exception;

import id.global.common.error.iris.MessagingError;

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
