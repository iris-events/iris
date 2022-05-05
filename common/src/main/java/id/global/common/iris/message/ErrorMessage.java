package id.global.common.iris.message;

import id.global.common.iris.error.ErrorType;
import id.global.common.iris.error.MessagingError;

public record ErrorMessage(ErrorType errorType, String code, String message) {
    public ErrorMessage(final MessagingError messagingError, final String message) {
        this(messagingError.getType(), messagingError.getClientCode(), message);
    }
}
