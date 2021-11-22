package id.global.event.messaging.it.sync;

import static id.global.common.annotations.amqp.ExchangeType.DIRECT;
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

import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.MessageHandler;
import id.global.event.messaging.runtime.exception.AmqpSendException;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DirectTestIT {
    private static final String EVENT_PAYLOAD_NAME = "event";
    private static final String EVENT_PAYLOAD_NAME_PRIORITY = "priority";
    private static final String EVENT_PAYLOAD_NAME_BLANK = "blank";
    private static final long EVENT_PAYLOAD_AGE = 10L;
    private static final String EVENT_QUEUE = "test-eventqueue";
    private static final String EVENT_QUEUE_BLANK = "blank-eventqueue";
    private static final String EVENT_QUEUE_PRIORITY = "test-eventqueue-priority";
    private static final String EXCHANGE = "test-exchange";
    private static final String MINIMUM_EVENT_ID = "minimumEvent";

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
    public void publishMessageWithBlankExchange() {
        assertDoesNotThrow(() -> producer.send(new BlankExchangeEvent("blank", 1L)));
    }

    @Test
    @DisplayName("Two messages published to different queues should be delivered")
    void publishTwoMessagesToDifferentQueues_BothShouldBeDelivered() throws Exception {

        assertDoesNotThrow(() -> producer.send(new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE)));

        assertDoesNotThrow(() -> producer.send(new PrioritizedEvent(EVENT_PAYLOAD_NAME_PRIORITY, EVENT_PAYLOAD_AGE)));

        assertDoesNotThrow(() -> producer.send(new BlankExchangeEvent(EVENT_PAYLOAD_NAME_BLANK, EVENT_PAYLOAD_AGE)));

        assertDoesNotThrow(() -> producer.send(new MinimumEvent(MINIMUM_EVENT_ID)));

        PrioritizedEvent priorityEvent = service.getHandledPriorityEvent().get();
        Event event = service.getHandledEvent().get();
        BlankExchangeEvent blankExchangeEvent = service.getHandledBlankExchangeEvent().get();
        MinimumEvent minimumEvent = service.getHandledMinimumEvent().get();

        assertThat(TestHandlerService.count.get(), is(4));

        assertThat(priorityEvent.name(), is(EVENT_PAYLOAD_NAME_PRIORITY));
        assertThat(priorityEvent.age(), is(EVENT_PAYLOAD_AGE));
        assertThat(event.name(), is(EVENT_PAYLOAD_NAME));
        assertThat(event.age(), is(EVENT_PAYLOAD_AGE));
        assertThat(blankExchangeEvent.name(), is(EVENT_PAYLOAD_NAME_BLANK));
        assertThat(minimumEvent.id, is(MINIMUM_EVENT_ID));
    }

    @ApplicationScoped
    public static class TestHandlerService {
        private final CompletableFuture<Event> handledEvent = new CompletableFuture<>();
        private final CompletableFuture<PrioritizedEvent> handledPriorityEvent = new CompletableFuture<>();
        private final CompletableFuture<BlankExchangeEvent> handledBlankExchangeEvent = new CompletableFuture<>();
        private final CompletableFuture<MinimumEvent> handledMinimumEvent = new CompletableFuture<>();

        public static final AtomicInteger count = new AtomicInteger(0);

        public void reset() {
            count.set(0);
        }

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = EVENT_QUEUE)
        public void handle(Event event) {
            count.incrementAndGet();
            handledEvent.complete(event);
        }

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = EVENT_QUEUE_PRIORITY)
        public void handlePriority(PrioritizedEvent event) {
            count.incrementAndGet();
            handledPriorityEvent.complete(event);
        }

        @SuppressWarnings("unused")
        @MessageHandler(bindingKeys = EVENT_QUEUE_BLANK)
        public void handleBlankExchange(BlankExchangeEvent event) {
            count.incrementAndGet();
            handledBlankExchangeEvent.complete(event);
        }

        @SuppressWarnings("unused")
        @MessageHandler
        public void handleMinimum(MinimumEvent event) {
            count.incrementAndGet();
            handledMinimumEvent.complete(event);
        }

        public CompletableFuture<Event> getHandledEvent() {
            return handledEvent;
        }

        public CompletableFuture<PrioritizedEvent> getHandledPriorityEvent() {
            return handledPriorityEvent;
        }

        public CompletableFuture<BlankExchangeEvent> getHandledBlankExchangeEvent() {
            return handledBlankExchangeEvent;
        }

        public CompletableFuture<MinimumEvent> getHandledMinimumEvent() {
            return handledMinimumEvent;
        }
    }

    @Message(routingKey = EVENT_QUEUE, name = EXCHANGE, exchangeType = DIRECT)
    public record Event(String name, Long age) {
    }

    @Message(routingKey = EVENT_QUEUE_PRIORITY, name = EXCHANGE, exchangeType = DIRECT)
    public record PrioritizedEvent(String name, Long age) {
    }

    @Message(name = "blank-exchange-event", routingKey = EVENT_QUEUE_BLANK, exchangeType = DIRECT)
    public record BlankExchangeEvent(String name, Long age) {
    }

    @Message(name = "minimum-event", exchangeType = DIRECT)
    public record MinimumEvent(String id) {
    }

}
