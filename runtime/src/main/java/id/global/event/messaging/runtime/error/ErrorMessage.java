package id.global.event.messaging.runtime.error;

import id.global.event.messaging.runtime.api.error.MessagingError;

public record ErrorMessage(String status, String name, String message) {
    public ErrorMessage(final MessagingError messagingError, final String message) {
        this(messagingError.getStatus(), messagingError.getName(), message);
    }
}
