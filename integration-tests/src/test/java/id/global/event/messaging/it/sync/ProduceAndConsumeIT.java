package id.global.event.messaging.it.sync;

import static id.global.asyncapi.spec.enums.ExchangeType.DIRECT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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

        producer.publish(
                EXCHANGE,
                Optional.of(EVENT_QUEUE),
                DIRECT,
                new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE),
                false);

        producer.publish(
                EXCHANGE,
                Optional.of(EVENT_QUEUE_PRIORITY),
                DIRECT,
                new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE),
                false);

        Event e = service.getHandledPriorityEvent().get();
        Event e2 = service.getHandledEvent().get();
        assertEquals(EVENT_PAYLOAD_NAME, e.getName());
        assertEquals(EVENT_PAYLOAD_AGE, e.getAge());
        assertEquals(EVENT_PAYLOAD_NAME, e2.getName());
        assertEquals(EVENT_PAYLOAD_AGE, e2.getAge());
        assertEquals(2, TestHandlerService.count.get());
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

        @MessageHandler(queue = EVENT_QUEUE_PRIORITY, exchange = EXCHANGE)
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