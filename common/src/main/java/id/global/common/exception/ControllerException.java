package id.global.common.exception;

import javax.ws.rs.core.Response;
import java.util.UUID;

public class ControllerException extends RuntimeException {
    private final ResponseError error;
    private final String message;

    public ControllerException(ResponseError error) {
        this(error, null);
    }

    public ControllerException(ResponseError error, String message) {
        this.error = error;
        this.message = message;
    }

    public ResponseError getErrorCode() {
        return error;
    }

    public String getMessage() {
        if (this.message == null) {
            return error.statusType.getReasonPhrase();
        }
        return message;
    }

    public ErrorModel toEntity() {
        return new ErrorModel(getMessage(), UUID.randomUUID().toString(), error.ordinal(), error.name());
    }

    protected Response toResponse() {
        return Response
                .status(error.statusType.getStatusCode())
                .entity(toEntity())
                .build();
    }

}
