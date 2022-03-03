package id.global.event.messaging.runtime.api.error;

public enum ClientError implements MessagingError {

    ERR_BAD_REQUEST(ErrorType.BAD_REQUEST),
    ERR_NOT_FOUND(ErrorType.NOT_FOUND);

    private final ErrorType errorType;

    ClientError(final ErrorType errorType) {
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
