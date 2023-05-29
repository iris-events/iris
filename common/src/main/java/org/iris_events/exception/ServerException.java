package org.iris_events.exception;

import org.iris_events.common.ErrorType;

public class ServerException extends MessagingException {
    private static final ErrorType ERROR_TYPE = ErrorType.INTERNAL_SERVER_ERROR;
    private final boolean shouldNotifyFrontend;

    public ServerException(final String clientCode, final String message, boolean notifyFrontend) {
        super(ERROR_TYPE, clientCode, message);
        this.shouldNotifyFrontend = notifyFrontend;
    }

    public ServerException(final String clientCode, final String message, boolean notifyFrontend, final Throwable cause) {
        super(ERROR_TYPE, clientCode, message, cause);
        this.shouldNotifyFrontend = notifyFrontend;
    }

    public boolean shouldNotifyFrontend() {
        return shouldNotifyFrontend;
    }
}
