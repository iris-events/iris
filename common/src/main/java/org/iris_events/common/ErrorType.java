package org.iris_events.common;

public enum ErrorType {
    // security
    FORBIDDEN,
    UNAUTHORIZED,
    AUTHENTICATION_FAILED,
    // client
    BAD_PAYLOAD,
    NOT_FOUND,
    // server
    INTERNAL_SERVER_ERROR
}
