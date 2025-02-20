package org.iris_events.it;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.iris_events.it.base.RequestIdPropagationTest;
import org.iris_events.producer.EventProducer;

@Path("/test")
public class TestRestResource {
    @Inject
    EventProducer eventProducer;

    @GET
    @Path("/send")
    public Response sendMessage() {
        eventProducer.send(new RequestIdPropagationTest.HandledEvent("Message"));
        return Response.ok().build();
    }
}
