package id.global.iris.common.exception;

import id.global.iris.common.error.ErrorType;

public class ForbiddenException extends SecurityException {
    private static final ErrorType ERROR_TYPE = ErrorType.FORBIDDEN;

    public ForbiddenException(final String clientCode, final String message) {
        super(ERROR_TYPE, clientCode, message);
    }

    public ForbiddenException(final String clientCode, final String message, final Throwable cause) {
        super(ERROR_TYPE, clientCode, message, cause);
    }
}
