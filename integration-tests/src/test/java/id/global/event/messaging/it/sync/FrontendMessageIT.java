package id.global.event.messaging.it.sync;

import static id.global.event.messaging.runtime.consumer.AmqpConsumer.FRONTEND_MESSAGE_EXCHANGE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.MessageHandler;
import id.global.common.annotations.amqp.Scope;
import id.global.event.messaging.it.IsolatedEventContextTest;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FrontendMessageIT extends IsolatedEventContextTest {

    public static final String FRONTEND_REQUEST_EVENT_NAME = "frontend-request-event";
    public static final String FRONTEND_REQUEST_DIRECT_EVENT_NAME = "frontend-request-direct-event";
    public static final String UNREGISTERED_FRONTEND_REQUEST = "unregistered-request";

    @Inject
    HandlerService service;

    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    ObjectMapper objectMapper;

    private Channel channel;

    @BeforeEach
    void setUp() throws IOException {
        final var connection = rabbitMQClient.connect("FrontendMessageIT publisher");
        channel = connection.createChannel();
    }

    @Test
    void consumeFrontendMessage() throws Exception {
        final var event = new FrontendEvent(FRONTEND_REQUEST_EVENT_NAME, 10L);
        final var directEvent = new FrontendEvent(FRONTEND_REQUEST_DIRECT_EVENT_NAME, 10L);
        final AMQP.BasicProperties basicProperties = new AMQP.BasicProperties();

        channel.basicPublish(FRONTEND_MESSAGE_EXCHANGE,
                FRONTEND_REQUEST_EVENT_NAME,
                basicProperties,
                writeValueAsBytes(event));

        // should not disrupt, there is no binding
        channel.basicPublish(FRONTEND_MESSAGE_EXCHANGE,
                UNREGISTERED_FRONTEND_REQUEST,
                basicProperties,
                writeValueAsBytes(new UnregisteredFrontendEvent("unregistered", "data")));

        channel.basicPublish(FRONTEND_MESSAGE_EXCHANGE,
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
