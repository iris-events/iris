package id.global.iris.messaging.runtime.api.exception;

import id.global.iris.messaging.runtime.api.error.MessagingError;

/**
 * Amqp security exception.
 * Iris EDA will return the message to the client session immediately.
 */
public class SecurityException extends MessagingException {

    public SecurityException(final MessagingError messageError, final String message) {
        super(messageError, message);
    }

    public SecurityException(final MessagingError messageError, final String message, final Throwable cause) {
        super(messageError, message, cause);
    }
}
