package id.global.event.messaging.it.sync;

import static id.global.asyncapi.spec.enums.ExchangeType.DIRECT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.event.messaging.it.events.Event;
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
        boolean isPublished = producer.publish(null);
        assertThat(isPublished, is(false));
    }

    @Test
    @DisplayName("Publish message with blank exchange")
    public void publishMessage_WithBlankExchange() {
        boolean isPublished = producer.publish(new Event("blank", 1L), "", "WrongRoutingKey", DIRECT);
        assertThat(isPublished, is(false));
    }

    @Test
    @DisplayName("Two messages published to different queues should be delivered")
    void publishTwoMessagesToDifferentQueues_BothShouldBeDelivered() throws Exception {

        boolean isPublished1 = producer.publish(
                new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE),
                EXCHANGE,
                EVENT_QUEUE,
                DIRECT);

        boolean isPublished2 = producer.publish(
                new Event(EVENT_PAYLOAD_NAME_PRIORITY, EVENT_PAYLOAD_AGE),
                EXCHANGE,
                EVENT_QUEUE_PRIORITY,
                DIRECT);

        Event priorityEvent = service.getHandledPriorityEvent().get();
        Event event = service.getHandledEvent().get();

        assertThat(isPublished1, is(true));
        assertThat(isPublished2, is(true));

        assertThat(TestHandlerService.count.get(), is(2));

        assertThat(priorityEvent.name(), is(EVENT_PAYLOAD_NAME_PRIORITY));
        assertThat(priorityEvent.age(), is(EVENT_PAYLOAD_AGE));
        assertThat(event.name(), is(EVENT_PAYLOAD_NAME));
        assertThat(event.age(), is(EVENT_PAYLOAD_AGE));
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
