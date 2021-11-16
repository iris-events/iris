package id.global.event.messaging.runtime.exception;

public class AmqpTransactionRuntimeException extends RuntimeException {
    public AmqpTransactionRuntimeException(String message) {
        super(message);
    }

    public AmqpTransactionRuntimeException(final String message, final AmqpTransactionException e) {
        super(message, e);
    }
}
