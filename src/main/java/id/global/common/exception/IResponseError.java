package id.global.common.exception;

import javax.ws.rs.core.Response;

public interface IResponseError {
    Response.StatusType getStatusType();
}
