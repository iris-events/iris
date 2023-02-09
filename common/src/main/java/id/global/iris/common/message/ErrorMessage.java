package id.global.iris.common.message;

import id.global.iris.common.error.ErrorType;

public class ErrorMessage {
   private final ErrorType errorType;
   private final String code;
   private final String message;

    public ErrorMessage(final ErrorType errorType, final String code, final String message) {
        this.errorType = errorType;
        this.code = code;
        this.message = message;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
