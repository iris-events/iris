package id.global.event.messaging.it.sync;

import static id.global.asyncapi.spec.enums.ExchangeType.DIRECT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.ShutdownSignalException;

import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.event.messaging.it.events.Event;
import id.global.event.messaging.runtime.Common;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ListenerAndHandlerTestIT {

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
        Assertions.assertThrows(
                ShutdownSignalException.class,
                () -> {
                    producer.publish(
                            new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE),
                            UNKNOWN_EXCHANGE,
                            EVENT_QUEUE,
                            DIRECT);
                });
    }

    @Test
    @DisplayName("Message published that cannot be routed will be lost")
    void publishNonRoutableMessages()
            throws ExecutionException, InterruptedException, IOException {

        String INVOKE_MESSAGE = "Return Invoked";

        CompletableFuture<String> completedSignal = new CompletableFuture<>();

        producer.addReturnListener(EXCHANGE + "_" + UNKNOWN_QUEUE,
                (replyCode, replyText, exchange, routingKey, properties, body) -> completedSignal.complete(INVOKE_MESSAGE),
                null);

        boolean published = producer.publish(
                new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE),
                EXCHANGE,
                UNKNOWN_QUEUE,
                DIRECT);

        assertThat(published, is(true));
        assertThat(completedSignal.get(), is(INVOKE_MESSAGE));

    }

    @Test
    @DisplayName("Publishing message to unknown queue with return listener set, should invoke return callback")
    void publishToUnknownQueue() throws ExecutionException, InterruptedException, IOException {

        CompletableFuture<String> completedSignal = new CompletableFuture<>();

        producer.addReturnListener(EXCHANGE + "_" + UNKNOWN_QUEUE, null, returnMessage -> completedSignal.complete("FAIL"));

        boolean published = producer.publish(
                new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE),
                EXCHANGE,
                UNKNOWN_QUEUE,
                DIRECT);

        assertThat(published, is(true));
        assertThat(completedSignal.get(), is("FAIL"));
    }

    @Test
    @DisplayName("Publishing message to known exchange and queue and confirm listener set, should invoke handleAck")
    void publishInvokeHandleAck() throws ExecutionException, InterruptedException, IOException {

        CompletableFuture<String> completedSignal = new CompletableFuture<>();

        producer.addConfirmListeners(EXCHANGE + "_" + EVENT_QUEUE, new ConfirmListener() {
            @Override
            public void handleAck(long deliveryTag, boolean multiple) {
                completedSignal.complete("ACK");
            }

            @Override
            public void handleNack(long deliveryTag, boolean multiple) {
                completedSignal.complete("NACK");
            }
        });

        boolean published = producer.publish(
                new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE),
                EXCHANGE,
                EVENT_QUEUE,
                DIRECT);

        assertThat(published, is(true));
        assertThat(completedSignal.get(), is("ACK"));
    }

    @Test
    @DisplayName("Adding NULL as confirm and return listeners should not be accepted.")
    void testNullListeners() throws IOException {

        boolean confirmListenerAdded = producer.addConfirmListeners(Common.createChannelKey(EXCHANGE, EVENT_QUEUE), null);
        boolean returnListenerAdded = producer.addReturnListener(Common.createChannelKey(EXCHANGE, EVENT_QUEUE), null, null);

        assertThat(confirmListenerAdded, is(false));
        assertThat(returnListenerAdded, is(false));

    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class ListenerAndHandleService {
        private final CompletableFuture<Event> handledEvent = new CompletableFuture<>();

        public static final AtomicInteger count = new AtomicInteger(0);

        public void reset() {
            count.set(0);
        }

        @MessageHandler(queue = EVENT_QUEUE, exchange = EXCHANGE)
        public void handle(Event event) {
            count.incrementAndGet();
            handledEvent.complete(event);
        }

        public CompletableFuture<Event> getHandledEvent() {
            return handledEvent;
        }

    }

}
