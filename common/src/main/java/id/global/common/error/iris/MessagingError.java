package id.global.common.error.iris;

public interface MessagingError {
    ErrorType getType();

    String getClientCode();
}
