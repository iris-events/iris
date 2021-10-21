package id.global.event.messaging.runtime.exception;

public class AmqpSendException extends Exception {

    public AmqpSendException(String message) {
        super(message);
    }

    public AmqpSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
