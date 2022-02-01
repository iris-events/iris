package id.global.event.messaging.runtime.api.error;

public enum ClientError implements MessagingError {

    BAD_REQUEST("ERR_BAD_REQUEST"),
    NOT_FOUND("ERR_NOT_FOUND");

    private final String clientCode;

    ClientError(final String clientCode) {
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
