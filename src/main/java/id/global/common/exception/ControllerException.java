package id.global.common.exception;

import javax.ws.rs.core.Response;
import java.util.UUID;

public class ControllerException extends RuntimeException {
    private final IResponseError error;
    private final String message;

    public ControllerException(IResponseError error) {
        this(error, null);
    }

    public ControllerException(IResponseError error, String message) {
        this.error = error;
        this.message = message;
    }

    public IResponseError getErrorCode() {
        return error;
    }

    public String getMessage() {
        if (this.message == null) {
            return error.getStatusType().getReasonPhrase();
        }
        return message;
    }

    public ErrorModel toEntity() {
        return new ErrorModel(getMessage(), UUID.randomUUID().toString(), error.getStatusType().getStatusCode(), error.getName());
    }

    protected Response toResponse() {
        return Response
                .status(error.getStatusType().getStatusCode())
                .entity(toEntity())
                .build();
    }

}
