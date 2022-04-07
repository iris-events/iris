package id.global.common.error.iris;

public enum ErrorType {
    // security
    FORBIDDEN,
    UNAUTHORIZED,
    AUTHORIZATION_FAILED,
    // client
    BAD_REQUEST,
    NOT_FOUND,
    // server
    INTERNAL_SERVER_ERROR
}
