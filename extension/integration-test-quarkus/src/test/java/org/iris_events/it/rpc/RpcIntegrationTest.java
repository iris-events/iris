package org.iris_events.it.rpc;

import static org.hamcrest.MatcherAssert.assertThat;

import jakarta.inject.Inject;

import org.hamcrest.CoreMatchers;
import org.iris_events.exception.IrisSendException;
import org.iris_events.producer.EventProducer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RpcIntegrationTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(
                    RpcServer.class,
                    RpcTestResponse.class,
                    RpcTestRequest.class,
                    RpcTestRequestInvalid.class));

    @Inject
    EventProducer eventProducer;

    @Test
    public void testRpcCall() {

        final var rpcRequest = new RpcTestRequest("some-id");
        final var rpcResponse = eventProducer.sendAndReceive(rpcRequest, RpcTestResponse.class);

        assertThat(rpcResponse.id(), CoreMatchers.is(rpcRequest.id()));
    }

    @Test
    public void testRpcCallWithoutDefinedResponseClass() {
        final var rpcRequestInvalid = new RpcTestRequestInvalid("no-rpc-response-defined");
        Assertions.assertThrows(IrisSendException.class, () -> {
            final var rpcResponse = eventProducer.sendAndReceive(rpcRequestInvalid, RpcTestResponse.class);
        });
    }
}
