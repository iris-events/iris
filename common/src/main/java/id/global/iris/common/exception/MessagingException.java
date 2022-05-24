package id.global.iris.common.exception;

import id.global.iris.common.error.ErrorType;

public abstract class MessagingException extends RuntimeException {

    private final ErrorType errorType;
    private final String clientCode;

    MessagingException(final ErrorType errorType, final String clientCode, final String message) {
        super(message);
        this.errorType = errorType;
        this.clientCode = clientCode;
    }

    MessagingException(final ErrorType errorType, final String clientCode, final String message, final Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.clientCode = clientCode;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getClientCode() {
        return clientCode;
    }
}
