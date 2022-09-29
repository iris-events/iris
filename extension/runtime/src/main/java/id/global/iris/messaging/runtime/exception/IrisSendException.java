package id.global.iris.messaging.runtime.exception;

public class IrisSendException extends RuntimeException {
    public IrisSendException(String message) {
        super(message);
    }

    public IrisSendException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
