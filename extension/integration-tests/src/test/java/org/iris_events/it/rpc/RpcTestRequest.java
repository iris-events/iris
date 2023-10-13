package org.iris_events.it.rpc;

import org.iris_events.annotations.Message;

//@IrisGenerated
@Message(name = "rpc-test-request", rpcResponse = RpcTestResponse.class)
public record RpcTestRequest(String id) {
}
