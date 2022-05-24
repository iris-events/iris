package id.global.common.iris.error;

public interface MessagingError {
    ErrorType getType();

    String getClientCode();
}
