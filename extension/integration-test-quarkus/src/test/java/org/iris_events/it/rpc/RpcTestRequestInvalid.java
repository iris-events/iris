package org.iris_events.it.rpc;

import org.iris_events.annotations.Message;

@Message(name = "rpc-test-request-invalid")
public record RpcTestRequestInvalid(String id) {
}
