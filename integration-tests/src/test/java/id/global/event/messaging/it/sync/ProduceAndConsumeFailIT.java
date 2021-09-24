package id.global.event.messaging.it.sync;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Return;
import com.rabbitmq.client.ReturnCallback;
import com.rabbitmq.client.ReturnListener;

import id.global.event.messaging.it.events.Event;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import id.global.event.messaging.runtime.producer.ExchangeType;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProduceAndConsumeFailIT {
    public static final String EVENT_PAYLOAD_NAME = "name";
    public static final long EVENT_PAYLOAD_AGE = 10L;
    public static final String EVENT_QUEUE = "test_EventQueue";
    private static final String EXCHANGE = "test_exchange";
    private static final String UNKNOWN_EXCHANGE = "unknown_exchange";
    private static final String UNKNOWN_QUEUE = "unknown_queue";

    @Inject
    AmqpProducer producer;

    @Test
    void basicProduceUnknownExchangeTest() throws ExecutionException, InterruptedException {

        boolean published = producer.publish(
                UNKNOWN_EXCHANGE,
                Optional.of(EVENT_QUEUE),
                ExchangeType.DIRECT,
                new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE),
                true);

        assertFalse(published);
    }

    @Test
    void basicProduceUnknownQueueTestReturnListener() {

        CompletableFuture<String> c = new CompletableFuture<>();

        producer.addReturnListener(EXCHANGE + "_" + UNKNOWN_QUEUE, new ReturnListener() {
            @Override
            public void handleReturn(int replyCode, String replyText, String exchange, String routingKey,
                    AMQP.BasicProperties properties, byte[] body) throws IOException {
                c.complete("a");
            }
        }, null);

        boolean published = producer.publish(
                EXCHANGE,
                Optional.of(UNKNOWN_QUEUE),
                ExchangeType.DIRECT,
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
                ExchangeType.DIRECT,
                new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE),
                false);

        try {
            assertEquals(c.get(), "b");
        } catch (InterruptedException | ExecutionException e) {
            fail();
        }
    }

}
