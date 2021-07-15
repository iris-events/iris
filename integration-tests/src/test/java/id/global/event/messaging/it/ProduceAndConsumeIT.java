package id.global.event.messaging.it;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.event.messaging.it.events.Event;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProduceAndConsumeIT {
    public static final String EVENT_PAYLOAD_NAME = "name";
    public static final long EVENT_PAYLOAD_AGE = 10L;
    public static final String EVENT_QUEUE = "test_EventQueue";
    public static final String EVENT_QUEUE_PRIORITY = "test_EventQueue_priority";
    public static final String EXCHANGE = "test_exchange";

    @Inject
    AmqpProducer producer;

    @Inject
    TestHandlerService service;

    @BeforeEach
    public void setup() {
        producer.connect();
    }

    @Test
    void basicProduceConsumeTest() throws Exception {
        producer.publishDirect(EXCHANGE, EVENT_QUEUE_PRIORITY, new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE), null);

        assertEquals(EVENT_PAYLOAD_NAME, service.getHandledPriorityEvent().get().getName());
        assertEquals(EVENT_PAYLOAD_AGE, service.getHandledPriorityEvent().get().getAge());

        //TODO: do we need this assertion here?
        assertThrows(TimeoutException.class, () -> service.getHandledEvent().get(1000, TimeUnit.MILLISECONDS));
    }

    @ApplicationScoped
    public static class TestHandlerService {
        private final CompletableFuture<Event> handledEvent = new CompletableFuture<>();
        private final CompletableFuture<Event> handledPriorityEvent = new CompletableFuture<>();

        @MessageHandler(queue = EVENT_QUEUE, exchange = EXCHANGE)
        public void handle(Event event) {
            handledEvent.complete(event);
        }

        @MessageHandler(queue = EVENT_QUEUE_PRIORITY, exchange = EXCHANGE)
        public void handlePriority(Event event) {
            handledPriorityEvent.complete(event);
        }

        public CompletableFuture<Event> getHandledEvent() {
            return handledEvent;
        }

        public CompletableFuture<Event> getHandledPriorityEvent() {
            return handledPriorityEvent;
        }
    }

}
