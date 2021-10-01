package id.global.event.messaging.runtime.exception;

public class AmqpConnectionFactoryException extends RuntimeException {
    public AmqpConnectionFactoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
