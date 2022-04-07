package id.global.iris.messaging.runtime.api.exception;

import id.global.common.error.iris.ErrorType;
import id.global.common.error.iris.MessagingError;

public abstract class MessagingException extends RuntimeException {

    private MessagingError messagingError;

    private final ErrorType errorType;

    private final String code;

    protected MessagingException(final MessagingError messagingError, final String message) {
        super(message);
        this.messagingError = messagingError;
        this.errorType = messagingError.getType();
        this.code = messagingError.getClientCode();
    }

    protected MessagingException(final MessagingError messagingError, final String message, Throwable cause) {
        super(message, cause);
        this.messagingError = messagingError;
        this.errorType = messagingError.getType();
        this.code = messagingError.getClientCode();
    }

    public MessagingError getMessagingError() {
        return messagingError;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getCode() {
        return code;
    }
}
