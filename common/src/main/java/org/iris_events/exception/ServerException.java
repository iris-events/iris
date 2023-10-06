package org.iris_events.exception;

import org.iris_events.common.ErrorType;

public class ServerException extends MessagingException {
    private static final ErrorType ERROR_TYPE = ErrorType.INTERNAL_SERVER_ERROR;
    private final boolean shouldNotifyFrontend;

    private final boolean shouldRetry;

    public ServerException(final String clientCode, final String message, boolean notifyFrontend, boolean retry) {
        super(ERROR_TYPE, clientCode, message);
        this.shouldNotifyFrontend = notifyFrontend;
        this.shouldRetry = retry;
    }

    public ServerException(final String clientCode, final String message, boolean notifyFrontend, boolean retry,
            final Throwable cause) {
        super(ERROR_TYPE, clientCode, message, cause);
        this.shouldNotifyFrontend = notifyFrontend;
        this.shouldRetry = retry;
    }

    public ServerException(final String clientCode, final String message, boolean notifyFrontend) {
        this(clientCode, message, notifyFrontend, true);
    }

    public ServerException(final String clientCode, final String message, boolean notifyFrontend, final Throwable cause) {
        this(clientCode, message, notifyFrontend, true, cause);
    }

    public boolean shouldNotifyFrontend() {
        return shouldNotifyFrontend;
    }

    public boolean shouldRetry() {
        return shouldRetry;
    }
}
