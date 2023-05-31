package org.iris_events.exception;

import org.iris_events.common.ErrorType;

public class UnauthorizedException extends SecurityException {
    private static final ErrorType ERROR_TYPE = ErrorType.UNAUTHORIZED;

    public UnauthorizedException(final String clientCode, final String message, final Throwable cause) {
        super(ERROR_TYPE, clientCode, message, cause);
    }
}
