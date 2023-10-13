package org.iris_events.it.rpc;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.iris_events.producer.EventProducer;

@ApplicationScoped
public class RpcClient {
    private final EventProducer eventProducer;

    @Inject
    public RpcClient(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public RpcTestResponse callRpc(UUID id) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        return eventProducer.sendRpcRequest(new RpcTestRequest(id.toString()), RpcTestResponse.class);
    }
}
