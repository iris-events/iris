package id.global.event.messaging.it.sync;

import static id.global.asyncapi.runtime.util.GidAnnotationParser.camelToKebabCase;
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

import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.MessageHandler;
import id.global.common.annotations.amqp.Scope;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FrontendMessageIT {

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
        final var event = new FrontendEvent("name", 10L);
        final AMQP.BasicProperties basicProperties = new AMQP.BasicProperties();

        channel.basicPublish(FRONTEND_MESSAGE_EXCHANGE,
                camelToKebabCase(FrontendEvent.class.getSimpleName()),
                basicProperties,
                writeValueAsBytes(event));

        final var consumedEvent = service.getHandledEvent().get(5, TimeUnit.SECONDS);

        assertThat(consumedEvent, is(notNullValue()));
    }

    @ApplicationScoped
    public static class HandlerService {
        private final CompletableFuture<FrontendEvent> handledEvent = new CompletableFuture<>();

        @SuppressWarnings("unused")
        @MessageHandler
        public void handle(FrontendEvent event) {
            handledEvent.complete(event);
        }

        public CompletableFuture<FrontendEvent> getHandledEvent() {
            return handledEvent;
        }
    }

    @Message(name = "frontend-request-event", scope = Scope.FRONTEND)
    public record FrontendEvent(String name, Long age) {
    }

    private byte[] writeValueAsBytes(Object value) throws RuntimeException {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialize to json", e);
        }
    }
}
