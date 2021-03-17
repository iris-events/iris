package id.global.common.exception.client;

import javax.ws.rs.core.Response;

public class ClientApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private Response response;

    public ClientApiException() {
        super();
    }

    public ClientApiException(Response response) {
        super("Api response has status code " + response.getStatus());
        this.response = response;
    }

    public Response getResponse() {
        return this.response;
    }
}
