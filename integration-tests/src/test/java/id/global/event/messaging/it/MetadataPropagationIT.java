package id.global.event.messaging.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.rabbitmq.client.AMQP;

import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.event.messaging.it.events.Event;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import id.global.event.messaging.runtime.producer.ExchangeType;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetadataPropagationIT {

    private static final String EVENT_QUEUE1 = "queue1";
    private static final String EVENT_QUEUE2 = "queue2";
    private static final String EVENT_QUEUE3 = "queue3";
    private static final String EXCHANGE = "EXCHANGE";
    @Inject
    AmqpProducer producer1;

    @Inject
    Service1 s1;
    @Inject
    Service2 s2;
    @Inject
    Service3 s3;

    @Test
    void test() throws Exception {
        for (int i = 0; i < 5; i++) {
            CompletableFuture.runAsync(() -> {
                String u1 = UUID.randomUUID().toString();

                AMQP.BasicProperties p1 = new AMQP.BasicProperties().builder()
                        .correlationId(u1)
                        .build();
                producer1.publish(
                        EXCHANGE,
                        Optional.of(EVENT_QUEUE1),
                        ExchangeType.DIRECT,
                        new Event(u1, 0L),
                        false, p1);
            });

            CompletableFuture.runAsync(() -> {
                String u2 = UUID.randomUUID().toString();
                AMQP.BasicProperties p2 = new AMQP.BasicProperties().builder()
                        .correlationId(u2)
                        .build();
                producer1.publish(
                        EXCHANGE,
                        Optional.of(EVENT_QUEUE1),
                        ExchangeType.DIRECT,
                        new Event(u2, 0L),
                        false, p2);
            });
            CompletableFuture.runAsync(() -> {

                producer1.publish(
                        EXCHANGE,
                        Optional.of(EVENT_QUEUE1),
                        ExchangeType.DIRECT,
                        new Event("asdf", 0L),
                        false);
            });
        }
        Thread.sleep(4000);
        assertEquals(10, Service3.count.get());
    }

    @ApplicationScoped
    public static class Service1 {
        private final CompletableFuture<Event> handledEvent = new CompletableFuture<>();
        public static final AtomicInteger count = new AtomicInteger(0);
        private final AmqpProducer producer;
        private final EventContext eventContext;

        @Inject
        public Service1(EventContext eventContext, AmqpProducer producer) {
            this.producer = producer;
            this.eventContext = eventContext;
        }

        public void reset() {
            count.set(0);
        }

        @MessageHandler(queue = EVENT_QUEUE1, exchange = EXCHANGE)
        public void handle(Event event) {
            AMQP.BasicProperties amqpBasicProperties = this.eventContext.getAmqpBasicProperties();
            assertEquals(event.getName(), amqpBasicProperties.getCorrelationId());
            count.incrementAndGet();
            handledEvent.complete(event);
            producer.publish(EXCHANGE, Optional.of(EVENT_QUEUE2), ExchangeType.TOPIC, event, true);
        }

        public CompletableFuture<Event> getHandledEvent() {
            return handledEvent;
        }

    }

    @ApplicationScoped
    public static class Service2 {

        private final CompletableFuture<Event> handledEvent = new CompletableFuture<>();
        public static final AtomicInteger count = new AtomicInteger(0);
        private final EventContext eventContext;

        private final AmqpProducer producer;

        @Inject
        public Service2(EventContext eventContext, AmqpProducer producer) {
            this.producer = producer;
            this.eventContext = eventContext;
        }

        public void reset() {
            count.set(0);
        }

        @MessageHandler(queue = EVENT_QUEUE2, exchange = EXCHANGE)
        public void handle(Event event) {

            AMQP.BasicProperties amqpBasicProperties = this.eventContext.getAmqpBasicProperties();
            assertEquals(event.getName(), amqpBasicProperties.getCorrelationId());
            count.incrementAndGet();

            handledEvent.complete(event);
            producer.publish(EXCHANGE, Optional.of(EVENT_QUEUE3), ExchangeType.TOPIC, event, true);
        }

        public CompletableFuture<Event> getHandledEvent() {
            return handledEvent;
        }

    }

    @ApplicationScoped
    public static class Service3 {
        private final CompletableFuture<Event> handledEvent = new CompletableFuture<>();
        public static final AtomicInteger count = new AtomicInteger(0);
        private final EventContext eventContext;

        @Inject
        public Service3(EventContext eventContext) {
            this.eventContext = eventContext;
        }

        public void reset() {
            count.set(0);
        }

        @MessageHandler(queue = EVENT_QUEUE3, exchange = EXCHANGE)
        public void handle(Event event) {

            AMQP.BasicProperties amqpBasicProperties = this.eventContext.getAmqpBasicProperties();
            assertEquals(event.getName(), amqpBasicProperties.getCorrelationId());
            count.incrementAndGet();

            handledEvent.complete(event);
        }

        public CompletableFuture<Event> getHandledEvent() {
            return handledEvent;
        }

    }
}
