package id.global.event.messaging.it.async;

import static id.global.asyncapi.spec.enums.ExchangeType.DIRECT;
import static id.global.asyncapi.spec.enums.ExchangeType.TOPIC;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
import id.global.event.messaging.runtime.producer.AmqpAsyncProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetadataPropagationAsyncIT {

    private static final String EVENT_QUEUE1 = "queue1-async";
    private static final String EVENT_QUEUE2 = "queue2-async";
    private static final String EVENT_QUEUE3 = "queue3-async";
    private static final String EXCHANGE = "exchange-async";
    @Inject
    AmqpAsyncProducer producer1;

    @Inject
    Service1 s1;
    @Inject
    Service2 s2;
    @Inject
    Service3 s3;

    @Test
    void test() throws Exception {
        for (int i = 0; i < 5; i++) {

            final var uuid1 = UUID.randomUUID().toString();
            final var uuid2 = UUID.randomUUID().toString();

            AMQP.BasicProperties p1 = new AMQP.BasicProperties().builder()
                    .correlationId(uuid1)
                    .build();

            producer1.publishAsync(
                    EXCHANGE,
                    Optional.of(EVENT_QUEUE1),
                    DIRECT,
                    new Event(uuid1, 0L),
                    p1);

            AMQP.BasicProperties p2 = new AMQP.BasicProperties().builder()
                    .correlationId(uuid2)
                    .build();

            producer1.publishAsync(
                    EXCHANGE,
                    Optional.of(EVENT_QUEUE1),
                    DIRECT,
                    new Event(uuid2, 0L),
                    p2);

            producer1.publishAsync(
                    EXCHANGE,
                    Optional.of(EVENT_QUEUE1),
                    DIRECT,
                    new Event("asdf", 0L));

        }
        s3.getHandledEvent().get();
        assertThat(Service3.count.get(), is(10));
    }

    @ApplicationScoped
    public static class Service1 {
        private final CompletableFuture<Event> handledEvent = new CompletableFuture<>();
        public static final AtomicInteger count = new AtomicInteger(0);
        private final AmqpAsyncProducer producer;
        private final EventContext eventContext;

        @Inject
        public Service1(EventContext eventContext, AmqpAsyncProducer producer) {
            this.producer = producer;
            this.eventContext = eventContext;
        }

        public void reset() {
            count.set(0);
        }

        @MessageHandler(queue = EVENT_QUEUE1, exchange = EXCHANGE)
        public void handle(Event event) {
            AMQP.BasicProperties amqpBasicProperties = this.eventContext.getAmqpBasicProperties();
            if (event.getName().equalsIgnoreCase(amqpBasicProperties.getCorrelationId())) {
                count.incrementAndGet();
                handledEvent.complete(event);
                producer.publishAsync(EXCHANGE, Optional.of(EVENT_QUEUE2), TOPIC, event);
            }
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

        private final AmqpAsyncProducer producer;

        @Inject
        public Service2(EventContext eventContext, AmqpAsyncProducer producer) {
            this.producer = producer;
            this.eventContext = eventContext;
        }

        public void reset() {
            count.set(0);
        }

        @MessageHandler(queue = EVENT_QUEUE2, exchange = EXCHANGE)
        public void handle(Event event) {

            AMQP.BasicProperties amqpBasicProperties = this.eventContext.getAmqpBasicProperties();
            if (event.getName().equalsIgnoreCase(amqpBasicProperties.getCorrelationId())) {
                count.incrementAndGet();
                handledEvent.complete(event);
                producer.publishAsync(EXCHANGE, Optional.of(EVENT_QUEUE3), TOPIC, event);
            }
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
            if (event.getName().equalsIgnoreCase(amqpBasicProperties.getCorrelationId())) {
                if (count.incrementAndGet() == 10) {
                    handledEvent.complete(event);
                }
            }
        }

        public CompletableFuture<Event> getHandledEvent() {
            return handledEvent;
        }

    }
}
