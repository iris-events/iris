package id.global.iris.messaging.runtime.requeue;

import id.global.common.iris.error.MessagingError;

public record MessagingErrorContext(MessagingError messagingError, String exceptionMessage) {
}
