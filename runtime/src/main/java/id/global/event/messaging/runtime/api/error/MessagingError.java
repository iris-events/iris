package id.global.event.messaging.runtime.api.error;

public interface MessagingError {
    ErrorType getType();

    String getClientCode();
}
