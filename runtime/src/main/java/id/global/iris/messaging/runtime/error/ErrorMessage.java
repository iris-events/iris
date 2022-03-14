package id.global.iris.messaging.runtime.error;

import id.global.iris.messaging.runtime.api.error.ErrorType;
import id.global.iris.messaging.runtime.api.error.MessagingError;

public record ErrorMessage(ErrorType errorType, String code, String message) {
    public ErrorMessage(final MessagingError messagingError, final String message) {
        this(messagingError.getType(), messagingError.getClientCode(), message);
    }
}
