package org.iris_events.it.sync;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.iris_events.it.IsolatedEventContextTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.MessageHandler;
import org.iris_events.annotations.Scope;
import org.iris_events.common.constants.Exchanges;
import org.iris_events.runtime.channel.ChannelService;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FrontendMessageIT extends IsolatedEventContextTest {

    public static final String FRONTEND_REQUEST_EVENT_NAME = "frontend-request-event";
    public static final String FRONTEND_REQUEST_DIRECT_EVENT_NAME = "frontend-request-direct-event";
    public static final String UNREGISTERED_FRONTEND_REQUEST = "unregistered-request";
    private static final String FRONTEND_EXCHANGE = Exchanges.FRONTEND.getValue();

    @Inject
    HandlerService service;

    @Inject
    @Named("consumerChannelService")
    ChannelService channelService;

    @Inject
    ObjectMapper objectMapper;

    private Channel channel;

    @BeforeEach
    void setUp() throws IOException {
        channel = channelService.createChannel();
    }

    @Test
    void consumeFrontendMessage() throws Exception {
        final var event = new FrontendEvent(FRONTEND_REQUEST_EVENT_NAME, 10L);
        final var directEvent = new FrontendEvent(FRONTEND_REQUEST_DIRECT_EVENT_NAME, 10L);
        final AMQP.BasicProperties basicProperties = new AMQP.BasicProperties();

        channel.basicPublish(FRONTEND_EXCHANGE,
                FRONTEND_REQUEST_EVENT_NAME,
                basicProperties,
                writeValueAsBytes(event));

        // should not disrupt, there is no binding
        channel.basicPublish(FRONTEND_EXCHANGE,
                UNREGISTERED_FRONTEND_REQUEST,
                basicProperties,
                writeValueAsBytes(new UnregisteredFrontendEvent("unregistered", "data")));

        channel.basicPublish(FRONTEND_EXCHANGE,
                FRONTEND_REQUEST_DIRECT_EVENT_NAME,
                basicProperties,
                writeValueAsBytes(directEvent));

        final var consumedEvent = service.getHandledEvent().get(5, TimeUnit.SECONDS);
        final var consumedDirectEvent = service.getHandledDirectEvent().get(5, TimeUnit.SECONDS);

        assertThat(consumedEvent, is(notNullValue()));
        assertThat(consumedDirectEvent, is(notNullValue()));
    }

    @ApplicationScoped
    public static class HandlerService {
        private final CompletableFuture<FrontendEvent> handledEvent = new CompletableFuture<>();
        private final CompletableFuture<FrontendDirectEvent> handledDirectEvent = new CompletableFuture<>();

        @SuppressWarnings("unused")
        @MessageHandler
        public void handle(FrontendEvent event) {
            handledEvent.complete(event);
        }

        @SuppressWarnings("unused")

        @MessageHandler
        public void handle(FrontendDirectEvent event) {
            handledDirectEvent.complete(event);
        }

        public CompletableFuture<FrontendEvent> getHandledEvent() {
            return handledEvent;
        }

        public CompletableFuture<FrontendDirectEvent> getHandledDirectEvent() {
            return handledDirectEvent;
        }
    }

    @Message(name = FRONTEND_REQUEST_EVENT_NAME, scope = Scope.FRONTEND)
    public record FrontendEvent(String name, Long age) {
    }

    /**
     * Frontend event will always be sent to frontend TOPIC exchange regardless of exchangeType set here.
     */
    @Message(name = FRONTEND_REQUEST_DIRECT_EVENT_NAME, scope = Scope.FRONTEND, exchangeType = ExchangeType.DIRECT)
    public record FrontendDirectEvent(String name, Long age) {
    }

    public record UnregisteredFrontendEvent(String name, String data) {
    }

    private byte[] writeValueAsBytes(Object value) throws RuntimeException {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialize to json", e);
        }
    }
}
