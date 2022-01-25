package id.global.event.messaging.runtime.api.error;

public enum SecurityError implements MessagingError {

    FORBIDDEN("FORBIDDEN", "ERR_FORBIDDEN"),
    UNAUTHORIZED("UNAUTHORIZED", "ERR_UNAUTHORIZED"),
    AUTHORIZATION_FAILED("UNAUTHORIZED", "ERR_AUTHORIZATION_FAILED");

    private final String status;
    private final String clientCode;

    SecurityError(final String status, final String clientCode) {
        this.status = status;
        this.clientCode = clientCode;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getClientCode() {
        return clientCode;
    }

    @Override
    public String getName() {
        return this.name();
    }
}
