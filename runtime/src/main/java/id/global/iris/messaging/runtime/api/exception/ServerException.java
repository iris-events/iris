package id.global.iris.messaging.runtime.api.exception;

import id.global.common.iris.error.MessagingError;

/**
 * Amqp retryable exception indicating server error.
 * Iris EDA system will automatically retry the message with configured retry policy.
 */
public class ServerException extends MessagingException {

    private final boolean notifyFrontend;

    public ServerException(final MessagingError messageError, final String message, boolean notifyFrontend) {
        super(messageError, message);
        this.notifyFrontend = notifyFrontend;
    }

    public ServerException(final MessagingError messageError, final String message, final Throwable cause,
            boolean notifyFrontend) {
        super(messageError, message, cause);
        this.notifyFrontend = notifyFrontend;
    }

    public boolean shouldNotifyFrontend() {
        return notifyFrontend;
    }
}
