package id.global.iris.messaging.it.context;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

import id.global.common.annotations.iris.Message;
import id.global.common.annotations.iris.MessageHandler;
import id.global.iris.messaging.it.IsolatedEventContextTest;
import id.global.iris.messaging.runtime.context.EventContext;
import id.global.iris.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventContextTest extends IsolatedEventContextTest {

    public static final String EVENT_NAME = "event-context-test-event";
    public static final String CUSTOM_HEADER_EVENT_NAME = "event-context-custom-header-test-event";
    public static final String CUSTOM_HEADER_NAME = "x-custom-header";
    public static final String CUSTOM_HEADER_VALUE = "custom-header-value";

    @Inject
    AmqpProducer producer;

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
        assertThat(basicProperties.getHeaders().get(CUSTOM_HEADER_NAME).toString(), is(CUSTOM_HEADER_VALUE));
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
            eventContext.setHeader(CUSTOM_HEADER_NAME, CUSTOM_HEADER_VALUE);
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