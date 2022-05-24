package id.global.iris.common.exception;

import id.global.iris.common.error.ErrorType;

public class NotFoundException extends ClientException {
    private static final ErrorType ERROR_TYPE = ErrorType.NOT_FOUND;

    public NotFoundException(final String clientCode, final String message) {
        super(ERROR_TYPE, clientCode, message);
    }

    public NotFoundException(final String clientCode, final String message, final Throwable cause) {
        super(ERROR_TYPE, clientCode, message, cause);
    }

    public NotFoundException(final String clientCode, final String resourceType, String resourceId) {
        super(ERROR_TYPE, clientCode, "resourceType: " + resourceType + ", resourceId: " + resourceId);
    }

    public NotFoundException(final String clientCode, final String resourceType, String resourceId,
            final Throwable cause) {
        super(ERROR_TYPE, clientCode, "resourceType: " + resourceType + ", resourceId: " + resourceId, cause);
    }
}
