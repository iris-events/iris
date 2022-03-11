package id.global.event.messaging.it.context;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.rabbitmq.client.Envelope;

import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.MessageHandler;
import id.global.event.messaging.it.IsolatedEventContextTest;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventContextTest extends IsolatedEventContextTest {

    public static final String EVENT_NAME = "event-context-test-event";

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
        assertThat(envelope.getExchange(), Is.is(EVENT_NAME));
    }

    @ApplicationScoped
    public static class TestHandlerService {
        @Inject
        EventContext eventContext;

        private final CompletableFuture<Envelope> handledEventEnvelope = new CompletableFuture<>();

        @SuppressWarnings("unused")
        @MessageHandler()
        public void handle(Event event) {
            handledEventEnvelope.complete(eventContext.getEnvelope());
        }

        public CompletableFuture<Envelope> getHandledEventEnvelope() {
            return handledEventEnvelope;
        }
    }

    @Message(name = EVENT_NAME)
    public record Event(String name) {
    }
}