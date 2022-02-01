package id.global.event.messaging.runtime.api.error;

public enum ServerError implements MessagingError {

    INTERNAL_SERVER_ERROR("ERR_SERVER_ERROR");

    private final String clientCode;

    ServerError(final String clientCode) {
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
