package id.global.event.messaging.runtime.exception;

public class AmqpSendException extends RuntimeException {
    public AmqpSendException(String message) {
        super(message);
    }

    public AmqpSendException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
