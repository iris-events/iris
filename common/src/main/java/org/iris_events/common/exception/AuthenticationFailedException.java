package org.iris_events.common.exception;

import org.iris_events.common.error.ErrorType;

public class AuthenticationFailedException extends SecurityException {
    private static final ErrorType ERROR_TYPE = ErrorType.AUTHENTICATION_FAILED;

    public AuthenticationFailedException(final String clientCode, final String message) {
        super(ERROR_TYPE, clientCode, message);
    }

    public AuthenticationFailedException(final String clientCode, final String message, final Throwable cause) {
        super(ERROR_TYPE, clientCode, message, cause);
    }
}
