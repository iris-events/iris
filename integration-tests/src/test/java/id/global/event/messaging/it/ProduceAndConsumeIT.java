package id.global.event.messaging.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.event.messaging.it.events.Event;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProduceAndConsumeIT {
    public static final String EVENT_PAYLOAD_NAME = "name";
    public static final long EVENT_PAYLOAD_AGE = 10L;
    public static final String EVENT_QUEUE = "test_EventQueue";
    public static final String EVENT_QUEUE_PRIORITY = "test_EventQueue_priority";
    public static final String EXCHANGE = "test_exchange";
    public static final String EXCHANGE_ADDITIONAL = "test_exchange_additional";

    @Inject
    AmqpProducer producer;

    @Inject
    TestHandlerService service;

    @BeforeEach
    public void setup() {
        service.reset();
    }

    @Test
    void basicProduceConsumeTest() throws Exception {

        producer.publishDirect(
                EXCHANGE,
                Optional.of(EVENT_QUEUE),
                new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE),
                null);

        producer.publishDirect(
                EXCHANGE_ADDITIONAL,
                Optional.of(EVENT_QUEUE_PRIORITY),
                new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE),
                null);

        assertEquals(EVENT_PAYLOAD_NAME, service.getHandledPriorityEvent().get(1000, TimeUnit.MILLISECONDS).getName());
        assertEquals(EVENT_PAYLOAD_AGE, service.getHandledPriorityEvent().get(1000, TimeUnit.MILLISECONDS).getAge());
        assertEquals(2, service.count.get());
    }

    @Test
    void basicProduceConsumeAsyncTest() throws Exception {

        for (int i = 0; i < 100; i++)
            producer.publishDirectAsync(
                    EXCHANGE,
                    Optional.of(EVENT_QUEUE),
                    new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE),
                    null);

        Thread.sleep(100);
        assertEquals(EVENT_PAYLOAD_NAME, service.getHandledPriorityEvent().get(1000, TimeUnit.MILLISECONDS).getName());
        assertEquals(EVENT_PAYLOAD_AGE, service.getHandledPriorityEvent().get(1000, TimeUnit.MILLISECONDS).getAge());
        assertEquals(100, service.count.get());
    }

    @ApplicationScoped
    public static class TestHandlerService {
        private final CompletableFuture<Event> handledEvent = new CompletableFuture<>();
        private final CompletableFuture<Event> handledPriorityEvent = new CompletableFuture<>();

        public static final AtomicInteger count = new AtomicInteger(0);

        public void reset() {
            count.set(0);
        }

        @MessageHandler(queue = EVENT_QUEUE, exchange = EXCHANGE)
        public void handle(Event event) {
            count.incrementAndGet();
            handledEvent.complete(event);
        }

        @MessageHandler(queue = EVENT_QUEUE_PRIORITY, exchange = EXCHANGE_ADDITIONAL)
        public void handlePriority(Event event) {
            count.incrementAndGet();
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
