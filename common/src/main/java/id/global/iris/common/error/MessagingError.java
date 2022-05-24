package id.global.iris.common.error;

public interface MessagingError {
    ErrorType getType();

    String getClientCode();
}
