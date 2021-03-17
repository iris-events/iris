package id.global.common.exception;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Tomaz Cerar 2020-08-05
 */
class ErrorModel {
    public final String message;
    @JsonProperty("error_id")
    public final String errorId;
    public final int statusCode;
    @JsonProperty("error_code")
    public final String errorCode;

    public ErrorModel(String message, String errorId, int statusCode, String errorCode) {
        this.message = message;
        this.errorId = errorId;
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public ErrorModel(String message, int statusCode, String errorCode) {
        this.message = message;
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.errorId = null;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorId() {
        return errorId;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
