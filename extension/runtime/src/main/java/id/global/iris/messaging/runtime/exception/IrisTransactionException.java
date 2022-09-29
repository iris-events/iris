package id.global.iris.messaging.runtime.exception;

public class IrisTransactionException extends IrisSendException {
    public IrisTransactionException(String message) {
        super(message);
    }

    public IrisTransactionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
