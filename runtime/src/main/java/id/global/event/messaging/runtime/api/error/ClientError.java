package id.global.event.messaging.runtime.api.error;

public enum ClientError implements MessagingError {

    BAD_REQUEST("BAD_REQUEST", "ERR_BAD_REQUEST"),
    NOT_FOUND("NOT_FOUND", "ERR_NOT_FOUND");

    private final String status;
    private final String clientCode;

    ClientError(final String status, final String clientCode) {
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
