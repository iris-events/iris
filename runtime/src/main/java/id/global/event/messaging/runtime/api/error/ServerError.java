package id.global.event.messaging.runtime.api.error;

public enum ServerError implements MessagingError {

    SERVER_ERROR("INTERNAL_SERVER_ERROR", "ERR_SERVER_ERROR");

    private final String status;
    private final String clientCode;

    ServerError(final String status, final String clientCode) {
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
