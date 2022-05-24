package id.global.iris.common.exception;

import id.global.iris.common.error.ErrorType;

public class BadPayloadException extends ClientException {
    private static final ErrorType ERROR_TYPE = ErrorType.BAD_PAYLOAD;

    public BadPayloadException(final String clientCode, final String message) {
        super(ERROR_TYPE, clientCode, message);
    }

    public BadPayloadException(final String clientCode, final String message, final Throwable cause) {
        super(ERROR_TYPE, clientCode, message, cause);
    }
}
