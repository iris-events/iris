package id.global.common.exception.client;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;


@Provider
public class ClientApiExceptionMapper implements ResponseExceptionMapper<ClientApiException> {

    @Override
    public boolean handles(int status, MultivaluedMap<String, Object> headers) {
        return status >= 400;
    }

    @Override
    public ClientApiException toThrowable(Response response) {
        return new ClientApiException(response);
    }
}
