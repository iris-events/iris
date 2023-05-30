package org.iris_events.it.context;

import static org.iris_events.common.MessagingHeaders.Message.SUBSCRIPTION_ID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.iris_events.it.IsolatedEventContextTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

import org.iris_events.annotations.Message;
import org.iris_events.annotations.MessageHandler;
import org.iris_events.context.EventContext;
import org.iris_events.producer.EventProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventContextTest extends IsolatedEventContextTest {

    public static final String EVENT_NAME = "event-context-test-event";
    public static final String CUSTOM_HEADER_EVENT_NAME = "event-context-custom-header-test-event";
    public static final String SUBSCRIPTION_ID_HEADER_VALUE = UUID.randomUUID().toString();

    @Inject
    EventProducer producer;

    @Inject
    TestHandlerService service;

    @Test
    void getEnvelope() throws Exception {
        final var event = new Event(UUID.randomUUID().toString());

        producer.send(event);

        final var envelope = service.getHandledEventEnvelope().get(5, TimeUnit.SECONDS);
        assertThat(envelope, notNullValue());
        assertThat(envelope.getExchange(), is(EVENT_NAME));
    }

    @Test
    void setHeader() throws Exception {
        final var event = new CustomHeaderEvent(UUID.randomUUID().toString());

        producer.send(event);

        final var basicProperties = service.getBasicPropertiesCompletableFuture().get(5, TimeUnit.SECONDS);
        assertThat(basicProperties, notNullValue());
        assertThat(basicProperties.getHeaders().get(SUBSCRIPTION_ID).toString(), is(SUBSCRIPTION_ID_HEADER_VALUE));
    }

    @ApplicationScoped
    public static class TestHandlerService {
        @Inject
        EventContext eventContext;

        private final CompletableFuture<Envelope> handledEventEnvelope = new CompletableFuture<>();
        private final CompletableFuture<AMQP.BasicProperties> basicPropertiesCompletableFuture = new CompletableFuture<>();

        @SuppressWarnings("unused")
        @MessageHandler()
        public void handle(Event event) {
            handledEventEnvelope.complete(eventContext.getEnvelope());
        }

        @SuppressWarnings("unused")
        @MessageHandler()
        public void handle(CustomHeaderEvent customHeaderEvent) {
            eventContext.setSubscriptionId(SUBSCRIPTION_ID_HEADER_VALUE);
            basicPropertiesCompletableFuture.complete(eventContext.getAmqpBasicProperties());
        }

        public CompletableFuture<Envelope> getHandledEventEnvelope() {
            return handledEventEnvelope;
        }

        public CompletableFuture<AMQP.BasicProperties> getBasicPropertiesCompletableFuture() {
            return basicPropertiesCompletableFuture;
        }
    }

    @Message(name = EVENT_NAME)
    public record Event(String name) {
    }

    @Message(name = CUSTOM_HEADER_EVENT_NAME)
    public record CustomHeaderEvent(String name) {
    }
}
