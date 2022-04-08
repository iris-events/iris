package id.global.common.error.iris;

public enum SecurityError implements MessagingError {

    FORBIDDEN(ErrorType.FORBIDDEN),
    UNAUTHORIZED(ErrorType.UNAUTHORIZED),
    AUTHORIZATION_FAILED(ErrorType.AUTHORIZATION_FAILED);

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
