package id.global.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class ErrorMapper implements javax.ws.rs.ext.ExceptionMapper<ControllerException> {
    private static final Logger log = LoggerFactory.getLogger("rest.error");

    @Override
    public Response toResponse(ControllerException e) {
        log.error("ControllerError: {}, {}", e.getErrorCode(), e.getMessage(), e);
        return e.toResponse();
    }
}
