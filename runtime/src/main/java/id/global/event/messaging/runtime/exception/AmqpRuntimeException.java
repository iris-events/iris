package id.global.event.messaging.runtime.exception;

public class AmqpRuntimeException extends RuntimeException {
    public AmqpRuntimeException(String message) {
        super(message);
    }

    public AmqpRuntimeException(final String message, final AmqpSendException e) {
        super(message, e);
    }
}
