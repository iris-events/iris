package id.global.event.messaging.runtime.error;

import id.global.event.messaging.runtime.api.error.IMessagingError;

public record ErrorMessage(String status, String name, String message) {
    public ErrorMessage(final IMessagingError messagingError, final String message) {
        this(messagingError.getStatus(), messagingError.getName(), message);
    }
}
