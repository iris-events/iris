package id.global.event.messaging.runtime.api.exception;

import id.global.event.messaging.runtime.api.error.IMessagingError;

/**
 * Amqp security exception.
 * Iris EDA will return the message to the client session immediately.
 */
public class SecurityException extends MessagingException {

    public SecurityException(final IMessagingError messageError, final String message) {
        super(messageError, message);
    }

    public SecurityException(final IMessagingError messageError, final String message, final Throwable cause) {
        super(messageError, message, cause);
    }
}
