package org.iris_events.it.rpc;

import jakarta.enterprise.context.ApplicationScoped;

import org.iris_events.annotations.MessageHandler;

@ApplicationScoped
public class RpcServer {

    @MessageHandler
    public RpcTestResponse rpcCall(RpcTestRequest testRequest) {
        return new RpcTestResponse(testRequest.id());
    }
}
