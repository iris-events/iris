package id.global.event.messaging.runtime.exception;

public class AmqpTransactionException extends Exception {

    public AmqpTransactionException(String message) {
        super(message);
    }

    public AmqpTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
