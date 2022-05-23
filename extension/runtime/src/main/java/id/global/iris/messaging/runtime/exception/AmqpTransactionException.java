package id.global.iris.messaging.runtime.exception;

public class AmqpTransactionException extends AmqpSendException {
    public AmqpTransactionException(String message) {
        super(message);
    }

    public AmqpTransactionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
