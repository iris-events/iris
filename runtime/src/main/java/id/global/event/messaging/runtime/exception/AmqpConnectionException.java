package id.global.event.messaging.runtime.exception;

public class AmqpConnectionException extends RuntimeException {
    public AmqpConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public AmqpConnectionException(String message) {
        super(message);
    }
}
