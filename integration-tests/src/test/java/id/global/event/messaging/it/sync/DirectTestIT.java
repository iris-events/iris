package id.global.event.messaging.it.sync;

import static id.global.asyncapi.spec.enums.ExchangeType.DIRECT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import id.global.asyncapi.spec.annotations.ConsumedEvent;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.event.messaging.runtime.exception.AmqpSendException;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DirectTestIT {
    private static final String EVENT_PAYLOAD_NAME = "name";
    private static final String EVENT_PAYLOAD_NAME_PRIORITY = "name2";
    private static final long EVENT_PAYLOAD_AGE = 10L;
    private static final String EVENT_QUEUE = "test-eventqueue";
    private static final String EVENT_QUEUE_PRIORITY = "test-eventqueue-priority";
    private static final String EXCHANGE = "test-exchange";

    @Inject
    AmqpProducer producer;

    @Inject
    TestHandlerService service;

    @BeforeEach
    public void setup() {
        service.reset();
    }

    @Test
    @DisplayName("Publish null message, should return false")
    void publishNullMessage() {
        assertThrows(AmqpSendException.class, () -> producer.send(null));
    }

    @Test
    @DisplayName("Publish message with blank exchange")
    public void publishMessage_WithBlankExchange() {
        assertThrows(AmqpSendException.class, () -> producer.send(new Event("blank", 1L), "", "WrongRoutingKey", DIRECT));
    }

    @Test
    @DisplayName("Two messages published to different queues should be delivered")
    void publishTwoMessagesToDifferentQueues_BothShouldBeDelivered() throws Exception {

        assertDoesNotThrow(() -> producer.send(
                new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE),
                EXCHANGE,
                EVENT_QUEUE,
                DIRECT));

        assertDoesNotThrow(() -> producer.send(
                new Event(EVENT_PAYLOAD_NAME_PRIORITY, EVENT_PAYLOAD_AGE),
                EXCHANGE,
                EVENT_QUEUE_PRIORITY,
                DIRECT));

        PrioritizedEvent priorityEvent = service.getHandledPriorityEvent().get();
        Event event = service.getHandledEvent().get();

        assertThat(TestHandlerService.count.get(), is(2));

        assertThat(priorityEvent.name(), is(EVENT_PAYLOAD_NAME_PRIORITY));
        assertThat(priorityEvent.age(), is(EVENT_PAYLOAD_AGE));
        assertThat(event.name(), is(EVENT_PAYLOAD_NAME));
        assertThat(event.age(), is(EVENT_PAYLOAD_AGE));
    }

    @ApplicationScoped
    public static class TestHandlerService {
        private final CompletableFuture<Event> handledEvent = new CompletableFuture<>();
        private final CompletableFuture<PrioritizedEvent> handledPriorityEvent = new CompletableFuture<>();

        public static final AtomicInteger count = new AtomicInteger(0);

        public void reset() {
            count.set(0);
        }

        @SuppressWarnings("unused")
        @MessageHandler
        public void handle(Event event) {
            count.incrementAndGet();
            handledEvent.complete(event);
        }

        @SuppressWarnings("unused")
        @MessageHandler
        public void handlePriority(PrioritizedEvent event) {
            count.incrementAndGet();
            handledPriorityEvent.complete(event);
        }

        public CompletableFuture<Event> getHandledEvent() {
            return handledEvent;
        }

        public CompletableFuture<PrioritizedEvent> getHandledPriorityEvent() {
            return handledPriorityEvent;
        }
    }

    @ConsumedEvent(queue = EVENT_QUEUE, exchange = EXCHANGE,exchangeType = DIRECT)
    public record Event(String name, Long age) {
    }

    @ConsumedEvent(queue = EVENT_QUEUE_PRIORITY, exchange = EXCHANGE, exchangeType = DIRECT)
    public record PrioritizedEvent(String name, Long age) {
    }
}
