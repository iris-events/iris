package id.global.iris.messaging.runtime.requeue;

import id.global.iris.common.error.MessagingError;

public record MessagingErrorContext(MessagingError messagingError, String exceptionMessage) {
}
