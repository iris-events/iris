package org.iris_events.common;

public enum ErrorType {
    // security
    UNAUTHORIZED,
    FORBIDDEN,
    // client
    BAD_PAYLOAD,
    NOT_FOUND,
    // server
    INTERNAL_SERVER_ERROR
}
