package id.global.event.messaging.it.sync;

import static id.global.asyncapi.spec.enums.ExchangeType.DIRECT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.rabbitmq.client.Return;
import com.rabbitmq.client.ReturnCallback;

import id.global.event.messaging.it.events.Event;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProduceAndConsumeFailIT {
    public static final String EVENT_PAYLOAD_NAME = "name";
    public static final long EVENT_PAYLOAD_AGE = 10L;
    public static final String EVENT_QUEUE = "test-event-queue";
    private static final String EXCHANGE = "test-exchange";
    private static final String UNKNOWN_EXCHANGE = "unknown-exchange";
    private static final String UNKNOWN_QUEUE = "unknown-queue";

    @Inject
    AmqpProducer producer;

    @Test
    void basicProduceUnknownExchangeTest() {

        boolean published = producer.publish(
                UNKNOWN_EXCHANGE,
                Optional.of(EVENT_QUEUE),
                DIRECT,
                new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE),
                true);

        assertFalse(published);
    }

    @Test
    void basicProduceUnknownQueueTestReturnListener() {

        CompletableFuture<String> c = new CompletableFuture<>();

        producer.addReturnListener(EXCHANGE + "_" + UNKNOWN_QUEUE,
                (replyCode, replyText, exchange, routingKey, properties, body) -> c.complete("a"), null);

        boolean published = producer.publish(
                EXCHANGE,
                Optional.of(UNKNOWN_QUEUE),
                DIRECT,
                new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE),
                false);

        try {
            assertEquals(c.get(), "a");
        } catch (InterruptedException | ExecutionException e) {
            fail();
        }
    }

    @Test
    void basicProduceUnknownQueueTestReturnCallback() {

        CompletableFuture<String> c = new CompletableFuture<>();

        producer.addReturnListener(EXCHANGE + "_" + UNKNOWN_QUEUE, null, new ReturnCallback() {
            @Override
            public void handle(Return returnMessage) {
                c.complete("b");
            }
        });

        boolean published = producer.publish(
                EXCHANGE,
                Optional.of(UNKNOWN_QUEUE),
                DIRECT,
                new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE),
                false);

        try {
            assertEquals(c.get(), "b");
        } catch (InterruptedException | ExecutionException e) {
            fail();
        }
    }

}
