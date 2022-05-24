package id.global.iris.common.exception;

import id.global.iris.common.error.ErrorType;

public abstract class ClientException extends MessagingException {
    public ClientException(final ErrorType errorType, final String clientCode, final String message) {
        super(errorType, clientCode, message);
    }

    public ClientException(final ErrorType errorType, final String clientCode, final String message, final Throwable cause) {
        super(errorType, clientCode, message, cause);
    }
}
