package id.global.iris.messaging.runtime.exception;

public class AmqpTransactionException extends RuntimeException {
    public AmqpTransactionException(String message) {
        super(message);
    }

    public AmqpTransactionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
