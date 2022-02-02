package id.global.event.messaging.runtime.api.error;

public enum SecurityError implements MessagingError {

    FORBIDDEN("ERR_FORBIDDEN"),
    UNAUTHORIZED("ERR_UNAUTHORIZED"),
    AUTHORIZATION_FAILED("ERR_AUTHORIZATION_FAILED");

    private final String clientCode;

    SecurityError(final String clientCode) {
        this.clientCode = clientCode;
    }

    @Override
    public String getStatus() {
        return this.name();
    }

    @Override
    public String getClientCode() {
        return clientCode;
    }

}
