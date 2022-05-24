package id.global.iris.common.error;

public enum ClientError implements MessagingError {

    BAD_REQUEST(ErrorType.BAD_REQUEST),
    NOT_FOUND(ErrorType.NOT_FOUND);

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
