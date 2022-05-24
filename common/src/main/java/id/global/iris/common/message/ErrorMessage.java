package id.global.iris.common.message;

import id.global.iris.common.error.ErrorType;
import id.global.iris.common.error.MessagingError;

public record ErrorMessage(ErrorType errorType, String code, String message) {
    public ErrorMessage(final MessagingError messagingError, final String message) {
        this(messagingError.getType(), messagingError.getClientCode(), message);
    }
}
