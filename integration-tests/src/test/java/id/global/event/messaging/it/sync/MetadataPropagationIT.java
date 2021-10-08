package id.global.event.messaging.it.sync;

import static id.global.asyncapi.spec.enums.ExchangeType.DIRECT;
import static id.global.asyncapi.spec.enums.ExchangeType.TOPIC;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.rabbitmq.client.AMQP;

import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.event.messaging.it.events.AnnotatedEvent;
import id.global.event.messaging.it.events.Event;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetadataPropagationIT {

    private static final String EVENT_QUEUE1 = "queue1";
    private static final String EVENT_QUEUE2 = "queue2";
    private static final String EVENT_QUEUE3 = "queue3";
    private static final String EXCHANGE = "exchange";

    @Inject
    AmqpProducer producer1;

    @Inject
    FinalService finalService;

    @Inject
    AnnotationService annotationService;

    @Test
    @DisplayName("Publish should work OOTB if annotated Event object is provided")
    void publishAnnotatedEvent() throws Exception {
        producer1.publish(new AnnotatedEvent("name", 1L));
        annotationService.getHandledEvent().get(1, TimeUnit.SECONDS);
        assertEquals(1, AnnotationService.count.get());
    }

    @Test
    @DisplayName("Event published should be accompanied with correlationId to the final service")
    void publishEventsWithCorrelationIds() throws Exception {
        for (int i = 0; i < 5; i++) {
            final var uuid1 = UUID.randomUUID().toString();
            final var uuid2 = UUID.randomUUID().toString();
            publishEvent(uuid1, uuid1);
            publishEvent(uuid2, uuid2);
            publishEvent("Event without correlationId");
        }
        finalService.getHandledEvent().get();
        assertEquals(10, Service1.count.get());
        assertEquals(10, Service2.count.get());
        assertEquals(10, FinalService.count.get());
    }

    private void publishEvent(final String name, final String correlationId) {
        final var amqpBasicProperties = Optional.ofNullable(correlationId).map(cId -> new AMQP.BasicProperties().builder()
                .correlationId(correlationId)
                .build()).orElse(null);

        CompletableFuture.runAsync(() -> producer1.publish(
                EXCHANGE,
                Optional.of(EVENT_QUEUE1),
                DIRECT,
                new Event(correlationId, 0L),
                false, amqpBasicProperties));
    }

    private void publishEvent(final String name) {
        CompletableFuture.runAsync(() -> producer1.publish(
                EXCHANGE,
                Optional.of(EVENT_QUEUE1),
                DIRECT,
                new Event(name, 0L),
                false));
    }

    @ApplicationScoped
    public static class AnnotationService {
        private final CompletableFuture<Event> handledEvent = new CompletableFuture<>();
        public static final AtomicInteger count = new AtomicInteger(0);

        @Inject
        public AnnotationService() {
        }

        @MessageHandler(queue = "annotated-queue", exchange = "annotated-exchange")
        public void handle(AnnotatedEvent event) {
            count.incrementAndGet();
            handledEvent.complete(new Event());
        }

        public CompletableFuture<Event> getHandledEvent() {
            return handledEvent;
        }

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

        @MessageHandler(queue = EVENT_QUEUE1, exchange = EXCHANGE)
        public void handle(Event event) {
            AMQP.BasicProperties amqpBasicProperties = this.eventContext.getAmqpBasicProperties();
            if (event.getName().equalsIgnoreCase(amqpBasicProperties.getCorrelationId())) {
                count.incrementAndGet();
                handledEvent.complete(event);
                producer.publish(EXCHANGE, Optional.of(EVENT_QUEUE2), TOPIC, event, true);
            }
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

        @MessageHandler(queue = EVENT_QUEUE2, exchange = EXCHANGE)
        public void handle(Event event) {

            AMQP.BasicProperties amqpBasicProperties = this.eventContext.getAmqpBasicProperties();
            if (event.getName().equalsIgnoreCase(amqpBasicProperties.getCorrelationId())) {
                count.incrementAndGet();
                handledEvent.complete(event);
                producer.publish(EXCHANGE, Optional.of(EVENT_QUEUE3), TOPIC, event, true);
            }
        }
    }

    @ApplicationScoped
    public static class FinalService {
        private final CompletableFuture<Event> handledEvent = new CompletableFuture<>();
        public static final AtomicInteger count = new AtomicInteger(0);
        private final EventContext eventContext;

        @Inject
        public FinalService(EventContext eventContext) {
            this.eventContext = eventContext;
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
