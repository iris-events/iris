package id.global.event.messaging.runtime.api.error;

public interface MessagingError {
    String getStatus();

    String getClientCode();

    String getName();
}
