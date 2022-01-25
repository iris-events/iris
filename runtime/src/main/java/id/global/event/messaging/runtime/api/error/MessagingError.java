package id.global.event.messaging.runtime.api.error;

public enum MessagingError implements IMessagingError {

    // security errors
    ERR_FORBIDDEN("FORBIDDEN"),
    ERR_UNAUTHORIZED("UNAUTHORIZED"),
    ERR_AUTHORIZATION_FAILED("UNAUTHORIZED"),

    // client errors
    ERR_BAD_REQUEST("BAD_REQUEST"),
    ERR_NOT_FOUND("NOT_FOUND"),

    // server errors
    ERR_SERVER_ERROR("INTERNAL_SERVER_ERROR");

    final String status;

    MessagingError(final String status) {
        this.status = status;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getName() {
        return this.name();
    }
}
