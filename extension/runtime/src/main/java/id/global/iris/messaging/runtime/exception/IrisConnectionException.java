package id.global.iris.messaging.runtime.exception;

public class IrisConnectionException extends RuntimeException {
    public IrisConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public IrisConnectionException(String message) {
        super(message);
    }
}
