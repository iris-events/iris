package id.global.event.messaging.runtime.api.exception;

import id.global.event.messaging.runtime.api.error.MessagingError;

public abstract class MessagingException extends RuntimeException {

    private MessagingError messagingError;

    private final String status;

    private final String name;

    protected MessagingException(final MessagingError messagingError, final String message) {
        super(message);
        this.messagingError = messagingError;
        this.status = messagingError.getStatus();
        this.name = messagingError.getName();
    }

    protected MessagingException(final MessagingError messagingError, final String message, Throwable cause) {
        super(message, cause);
        this.messagingError = messagingError;
        this.status = messagingError.getStatus();
        this.name = messagingError.getName();
    }

    public MessagingError getMessagingError() {
        return messagingError;
    }

    public String getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }
}
