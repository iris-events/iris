package id.global.event.messaging.it.sync;

import static id.global.common.annotations.amqp.ExchangeType.DIRECT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;

import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.MessageHandler;
import id.global.common.headers.amqp.MessagingHeaders;
import id.global.event.messaging.it.IsolatedEventContextTest;
import id.global.event.messaging.runtime.api.error.ServerError;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.exception.AmqpSendException;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import id.global.event.messaging.runtime.requeue.MessageRequeueHandler;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DirectTestIT extends IsolatedEventContextTest {
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

    @InjectMock
    MessageRequeueHandler requeueHandler;

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

        PrioritizedEvent priorityEvent = service.getHandledPriorityEvent().get(5, TimeUnit.SECONDS);
        Event event = service.getHandledEvent().get(5, TimeUnit.SECONDS);
        long eventTimestamp = service.getHandledEventTimestamp().get(5, TimeUnit.SECONDS);
        BlankExchangeEvent blankExchangeEvent = service.getHandledBlankExchangeEvent().get(5, TimeUnit.SECONDS);
        MinimumEvent minimumEvent = service.getHandledMinimumEvent().get(5, TimeUnit.SECONDS);

        assertThat(TestHandlerService.count.get(), is(4));

        assertThat(priorityEvent.name(), is(EVENT_PAYLOAD_NAME_PRIORITY));
        assertThat(priorityEvent.age(), is(EVENT_PAYLOAD_AGE));
        assertThat(event.name(), is(EVENT_PAYLOAD_NAME));
        assertThat(event.age(), is(EVENT_PAYLOAD_AGE));
        assertThat(blankExchangeEvent.name(), is(EVENT_PAYLOAD_NAME_BLANK));
        assertThat(minimumEvent.id, is(MINIMUM_EVENT_ID));
        assertThat(eventTimestamp, is(notNullValue()));
        assertThat(eventTimestamp < (new Date().getTime()), is(true));
    }

    @Test
    @DisplayName("Failed consume should redirect message to retry queue")
    void testDlqDelivery() throws Exception {
        String id = "id";
        String payload = "payload";

        producer.send(new FailEvent(id, payload));

        final var errorCodeCaptor = ArgumentCaptor.forClass(String.class);
        final var notifyFrontendCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(requeueHandler, timeout(500).times(1))
                .enqueueWithBackoff(any(), any(), errorCodeCaptor.capture(), notifyFrontendCaptor.capture());

        final var errorCode = errorCodeCaptor.getValue();
        final var notifyFrontend = notifyFrontendCaptor.getValue();

        assertThat(errorCode, is(ServerError.ERR_SERVER_ERROR.getClientCode()));
        assertThat(notifyFrontend, is(false));
    }

    @ApplicationScoped
    public static class TestHandlerService {
        @Inject
        EventContext eventContext;

        private final CompletableFuture<Event> handledEvent = new CompletableFuture<>();
        private final CompletableFuture<Long> handledEventTimestamp = new CompletableFuture<>();
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
            handledEventTimestamp.complete(
                    (Long) eventContext.getAmqpBasicProperties().getHeaders().get(MessagingHeaders.Message.SERVER_TIMESTAMP));
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

        @SuppressWarnings("unused")
        @MessageHandler
        public void handleFail(FailEvent failEvent) {
            count.incrementAndGet();
            throw new RuntimeException();
        }

        public CompletableFuture<Event> getHandledEvent() {
            return handledEvent;
        }

        public CompletableFuture<Long> getHandledEventTimestamp() {
            return handledEventTimestamp;
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

    @Message(name = "fail-event", exchangeType = DIRECT, deadLetter = "test-dead-letter")
    public record FailEvent(String id, String data) {
    }
}
