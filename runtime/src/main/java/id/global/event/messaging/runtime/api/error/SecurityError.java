package id.global.event.messaging.runtime.api.error;

public enum SecurityError implements MessagingError {

    ERR_FORBIDDEN(ErrorType.FORBIDDEN),
    ERR_UNAUTHORIZED(ErrorType.UNAUTHORIZED),
    ERR_AUTHORIZATION_FAILED(ErrorType.AUTHORIZATION_FAILED);

    private final ErrorType errorType;

    SecurityError(final ErrorType errorType) {
        this.errorType = errorType;
    }

    @Override
    public ErrorType getType() {
        return errorType;
    }

    @Override
    public String getClientCode() {
        return this.name();
    }

}
