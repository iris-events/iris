package id.global.iris.common.message;

import com.fasterxml.jackson.annotation.JsonProperty;

import id.global.iris.common.error.ErrorType;

public class ErrorMessage {
   private final @JsonProperty("error_type") ErrorType errorType;
   private final @JsonProperty("code") String code;
   private final @JsonProperty("message") String message;

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
