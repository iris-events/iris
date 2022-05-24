package id.global.common.exception;

import javax.ws.rs.core.Response;

/**
 * @author Tomaz Cerar 2020-08-05
 */
public enum ResponseError implements IResponseError {
    ERR_UNAUTHORIZED(Response.Status.UNAUTHORIZED),
    ERR_TOKEN(Response.Status.UNAUTHORIZED),
    ERR_USER_TOKEN_NOT_FOUND(Response.Status.NOT_FOUND),
    ERR_BAD_REQUEST(Response.Status.BAD_REQUEST),
    ERR_SERVER_ERROR(Response.Status.INTERNAL_SERVER_ERROR),
    ERR_CONSENT_COMPLETED(Response.Status.CONFLICT),
    ERR_SENDER_NOT_VALID(Response.Status.BAD_REQUEST),
    ERR_FORBIDDEN(Response.Status.FORBIDDEN),
    ;

    final Response.StatusType statusType;

    ResponseError(Response.StatusType statusType) {
        this.statusType = statusType;
    }

    @Override
    public Response.StatusType getStatusType() {
        return statusType;
    }

    @Override
    public int getOrdinal() {
        return this.ordinal();
    }

    @Override
    public String getName() {
        return this.name();
    }
}
