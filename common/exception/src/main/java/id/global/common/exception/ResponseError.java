package id.global.common.exception;

import javax.ws.rs.core.Response;

/**
 * @author Tomaz Cerar 2020-08-05
 */
public enum ResponseError {
    ERR_UNAUTHORIZED(Response.Status.UNAUTHORIZED),
    ERR_TOKEN(Response.Status.UNAUTHORIZED),
    ERR_WALLET_NOT_FOUND(Response.Status.NOT_FOUND),
    ERR_CONSENT_NOT_FOUND(Response.Status.NOT_FOUND),
    ERR_USER_TOKEN_NOT_FOUND(Response.Status.NOT_FOUND),
    ERR_UPHOLD_UNKNOWN(Response.Status.INTERNAL_SERVER_ERROR),
    ERR_UPHOLD_BAD_DATA(Response.Status.BAD_REQUEST),
    ERR_DUPLICATE_WALLET(Response.Status.CONFLICT),
    ERR_MINOR_PERSON(Response.Status.BAD_REQUEST),
    ERR_NAME_NOT_VALID(Response.Status.BAD_REQUEST),
    ERR_BAD_REQUEST(Response.Status.BAD_REQUEST),
    ERR_SERVER_ERROR(Response.Status.INTERNAL_SERVER_ERROR),
    ERR_CONSENT_COMPLETED(Response.Status.CONFLICT),
    ERR_CARD_ACCOUNT_NOT_FOUND(Response.Status.BAD_REQUEST),
    ERR_CARD_NOT_FOUND(Response.Status.NOT_FOUND),
    ERR_APTO_UNKNOWN(Response.Status.INTERNAL_SERVER_ERROR),
    ERR_DUPLICATE_CARD(Response.Status.CONFLICT),
    ERR_ACCOUNT_NOT_FOUND(Response.Status.NOT_FOUND),
    ERR_SENDER_NOT_VALID(Response.Status.BAD_REQUEST),
    ERR_FORBIDDEN(Response.Status.FORBIDDEN),
    ;

    final Response.StatusType statusType;

    ResponseError(final Response.StatusType statusType) {
        this.statusType = statusType;
    }

    public Response.StatusType getStatusType() {
        return statusType;
    }
}
