package id.global.event.messaging.it.async;

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
import id.global.event.messaging.runtime.producer.AmqpAsyncProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProduceAndConsumeAsyncIT {
    public static final String EVENT_PAYLOAD_NAME = "name";
    public static final String EVENT_QUEUE_PRIORITY = "ev-queue-async";
    public static final String EXCHANGE_ADDITIONAL = "ev-exchange-async";
    public static final int messageToSend = 100;

    private final AmqpAsyncProducer amqpAsyncProducer;

    private final TestHandlerService service;

    @Inject
    public ProduceAndConsumeAsyncIT(TestHandlerService service, AmqpAsyncProducer producer) {
        this.service = service;
        this.amqpAsyncProducer = producer;
    }

    @BeforeEach
    public void setup() {
        TestHandlerService.msgCount.set(0);
    }

    @Test
    void basicProduceConsumeAsyncTest() throws Exception {

        for (int i = 0; i < messageToSend; i++) {
            amqpAsyncProducer.publishAsync(
                    EXCHANGE_ADDITIONAL,
                    Optional.of(EVENT_QUEUE_PRIORITY),
                    DIRECT,
                    new Event(EVENT_PAYLOAD_NAME, (long) messageToSend));
        }

        Event e = service.getHandledPriorityEvent().get();

        assertEquals(EVENT_PAYLOAD_NAME, e.getName());
        assertEquals(messageToSend, e.getAge());
        assertEquals(messageToSend, TestHandlerService.msgCount.get());
    }

    @ApplicationScoped
    public static class TestHandlerService {

        private final CompletableFuture<Event> handledPriorityEvent = new CompletableFuture<>();

        public static final AtomicInteger msgCount = new AtomicInteger(0);

        @MessageHandler(queue = EVENT_QUEUE_PRIORITY, exchange = EXCHANGE_ADDITIONAL)
        public void handlePriority(final Event event) {
            final int c = msgCount.incrementAndGet();

            if (c == messageToSend) {
                handledPriorityEvent.complete(event);
            }
        }

        public CompletableFuture<Event> getHandledPriorityEvent() {
            return handledPriorityEvent;
        }
    }

}
