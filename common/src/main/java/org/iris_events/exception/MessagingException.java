package org.iris_events.exception;

import org.iris_events.common.ErrorType;

public abstract class MessagingException extends RuntimeException {

    private ErrorType errorType;
    private String clientCode;

    public MessagingException() {
        super();
    }

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

    public void setErrorType(final ErrorType errorType) {
        this.errorType = errorType;
    }

    public void setClientCode(final String clientCode) {
        this.clientCode = clientCode;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getClientCode() {
        return clientCode;
    }
}
