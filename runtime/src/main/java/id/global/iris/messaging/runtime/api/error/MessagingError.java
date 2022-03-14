package id.global.iris.messaging.runtime.api.error;

public interface MessagingError {
    ErrorType getType();

    String getClientCode();
}
