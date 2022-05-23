package id.global.iris.messaging.it.sync;

import static id.global.common.iris.annotations.ExchangeType.DIRECT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.ShutdownSignalException;

import id.global.common.iris.annotations.Message;
import id.global.common.iris.annotations.MessageHandler;
import id.global.iris.messaging.it.IsolatedEventContextTest;
import id.global.iris.messaging.runtime.channel.ChannelKey;
import id.global.iris.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ListenerAndHandlerTestIT extends IsolatedEventContextTest {

    private static final String EVENT_PAYLOAD_NAME = "name";
    private static final long EVENT_PAYLOAD_AGE = 10L;
    private static final String EVENT_QUEUE = "listener-queue";
    private static final String EXCHANGE = "listener-exchange";
    private static final String UNKNOWN_EXCHANGE = "unknown-exchange";
    private static final String UNKNOWN_QUEUE = "unknown-queue";

    @Inject
    AmqpProducer producer;

    @Test
    @DisplayName("Event published to unknown exchange should fail!")
    void publishToUnknownExchange() {
        Assertions.assertThrows(ShutdownSignalException.class,
                () -> producer.send(new UnknownExchangeEvent(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE)));
    }

    @Test
    @DisplayName("Message published that cannot be routed will be lost")
    void publishNonRoutableMessages()
            throws ExecutionException, InterruptedException {

        String INVOKE_MESSAGE = "Return Invoked";

        CompletableFuture<String> completedSignal = new CompletableFuture<>();

        assertDoesNotThrow(() -> producer.addReturnListener(EXCHANGE + "_" + UNKNOWN_QUEUE,
                (replyCode, replyText, exchange, routingKey, properties, body) -> completedSignal.complete(INVOKE_MESSAGE)));

        assertDoesNotThrow(
                () -> producer.send(new UnknownQueueEvent(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE)));
        assertThat(completedSignal.get(), is(INVOKE_MESSAGE));
    }

    @Test
    @DisplayName("Publishing message to unknown queue with return listener set, should invoke return callback")
    void publishToUnknownQueue() throws ExecutionException, InterruptedException, TimeoutException {

        CompletableFuture<String> completedSignal = new CompletableFuture<>();

        assertDoesNotThrow(() -> producer.addReturnCallback(EXCHANGE + "_" + UNKNOWN_QUEUE,
                returnMessage -> completedSignal.complete("FAIL")));

        assertDoesNotThrow(
                () -> producer.send(new UnknownQueueEvent(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE)));

        assertThat(completedSignal.get(5, TimeUnit.SECONDS), is("FAIL"));
    }

    @Test
    @DisplayName("Publishing message to known exchange and queue and confirm listener set, should invoke handleAck")
    void publishInvokeHandleAck() throws ExecutionException, InterruptedException, TimeoutException {

        CompletableFuture<String> completedSignal = new CompletableFuture<>();

        assertDoesNotThrow(() -> producer.addConfirmListener(EXCHANGE + "_" + EVENT_QUEUE, new ConfirmListener() {
            @Override
            public void handleAck(long deliveryTag, boolean multiple) {
                completedSignal.complete("ACK");
            }

            @Override
            public void handleNack(long deliveryTag, boolean multiple) {
                completedSignal.complete("NACK");
            }
        }));

        assertDoesNotThrow(() -> producer.send(new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE)));

        assertThat(completedSignal.get(5, TimeUnit.SECONDS), is("ACK"));
    }

    @Test
    @DisplayName("Adding NULL as confirm and return listeners should not be accepted.")
    void testNullListeners() {
        assertThrows(NullPointerException.class,
                () -> producer.addConfirmListener(ChannelKey.create(EXCHANGE, EVENT_QUEUE), null));

        assertThrows(NullPointerException.class,
                () -> producer.addReturnListener(ChannelKey.create(EXCHANGE, EVENT_QUEUE), null));

        assertThrows(NullPointerException.class,
                () -> producer.addReturnCallback(ChannelKey.create(EXCHANGE, EVENT_QUEUE), null));
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class ListenerAndHandleService {
        private final CompletableFuture<Event> handledEvent = new CompletableFuture<>();

        public static final AtomicInteger count = new AtomicInteger(0);

        public void reset() {
            count.set(0);
        }

        @MessageHandler(bindingKeys = EVENT_QUEUE)
        public void handle(Event event) {
            count.incrementAndGet();
            handledEvent.complete(event);
        }

        public CompletableFuture<Event> getHandledEvent() {
            return handledEvent;
        }
    }

    @Message(routingKey = EVENT_QUEUE, name = EXCHANGE, exchangeType = DIRECT)
    public record Event(String name, Long age) {
    }

    @Message(name = UNKNOWN_EXCHANGE, exchangeType = DIRECT, routingKey = EVENT_QUEUE)
    public record UnknownExchangeEvent(String name, Long age) {
    }

    @Message(name = EXCHANGE, exchangeType = DIRECT, routingKey = UNKNOWN_QUEUE)
    public record UnknownQueueEvent(String name, Long age) {
    }
}
