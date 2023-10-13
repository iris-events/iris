package org.iris_events.it.rpc;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RpcIntegrationTest {

    @Inject
    RpcClient rpcClient;

    @Test
    public void testRpcCall() throws InterruptedException, IOException, ExecutionException, TimeoutException {

        var id = UUID.randomUUID();
        final var rpcTestResponse = rpcClient.callRpc(id);

        assertThat(id.toString(), CoreMatchers.is(rpcTestResponse.id()));
    }
}
