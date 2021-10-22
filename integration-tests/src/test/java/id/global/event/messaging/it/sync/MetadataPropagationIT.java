package id.global.event.messaging.it.sync;

import static id.global.asyncapi.spec.enums.ExchangeType.DIRECT;
import static id.global.asyncapi.spec.enums.ExchangeType.TOPIC;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.rabbitmq.client.AMQP;

import id.global.asyncapi.spec.annotations.ConsumedEvent;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.annotations.ProducedEvent;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.exception.AmqpSendException;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import id.global.event.messaging.runtime.producer.MetadataInfo;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetadataPropagationIT {

    private static final String EVENT_QUEUE1 = "queue1";
    private static final String EVENT_QUEUE2 = "queue2";
    private static final String EVENT_QUEUE3 = "queue3";
    private static final String EXCHANGE = "exchange";

    @Inject
    AmqpProducer testProducer;

    @Inject
    FinalService finalService;

    @Inject
    Service1 service1;

    @Inject
    Service2 service2;

    @BeforeEach
    void setUp() {
        finalService.reset();
        service1.reset();
        service2.reset();
    }

    @Test
    @DisplayName("Publishing correctly annotated event with extra metadata, should send correctly")
    void publishedAnnotatedEventWithMetadata() throws ExecutionException, InterruptedException, TimeoutException {
        final String uuid = UUID.randomUUID().toString();
        finalService.setLimit(2);
        finalService.setLimit(1);

        MetadataInfo metadataInfo = new MetadataInfo(uuid);

        assertDoesNotThrow(() -> {
            testProducer.send(new AnnotatedEvent1(uuid, 1L), metadataInfo);
        });

        finalService.getHandledEvent().get(2, TimeUnit.SECONDS);
        assertThat(Service1.count.get(), is(1));
        assertThat(Service2.count.get(), is(1));
        assertThat(FinalService.count.get(), is(1));
    }

    @Test
    @DisplayName("Event published should be accompanied with correlationId to the final service")
    void publishPropagatesCorrelationId() throws Exception {
        for (int i = 0; i < 5; i++) {
            final var uuid1 = UUID.randomUUID().toString();
            final var uuid2 = UUID.randomUUID().toString();

            publishEvent(uuid1, uuid1);
            publishEvent(uuid2, uuid2);
            publishEvent("Event without correlationId");
        }
        finalService.getHandledEvent().get();
        assertThat(Service1.count.get(), is(10));
        assertThat(Service2.count.get(), is(10));
        assertThat(FinalService.count.get(), is(10));
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class Service1 {

        private CompletableFuture<Service1Event> handledEvent = new CompletableFuture<>();
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
            handledEvent = new CompletableFuture<>();
        }

        @MessageHandler
        public void handle(Service1Event event) throws AmqpSendException, IOException {
            AMQP.BasicProperties amqpBasicProperties = this.eventContext.getAmqpBasicProperties();
            if (event.name().equalsIgnoreCase(amqpBasicProperties.getCorrelationId())) {
                count.incrementAndGet();
                handledEvent.complete(event);
                producer.send(event, EXCHANGE, EVENT_QUEUE2, TOPIC);
            }
        }
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class Service2 {

        private CompletableFuture<Service2Event> handledEvent = new CompletableFuture<>();
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
            handledEvent = new CompletableFuture<>();
        }

        @MessageHandler
        public void handle(Service2Event event) throws AmqpSendException, IOException {

            AMQP.BasicProperties amqpBasicProperties = this.eventContext.getAmqpBasicProperties();
            if (event.name().equalsIgnoreCase(amqpBasicProperties.getCorrelationId())) {
                count.incrementAndGet();
                handledEvent.complete(event);
                producer.send(event, EXCHANGE, EVENT_QUEUE3, TOPIC);
            }
        }
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class FinalService {
        private CompletableFuture<FinalServiceEvent> handledEvent = new CompletableFuture<>();
        public static final AtomicInteger count = new AtomicInteger(0);
        private final EventContext eventContext;
        private static int limit = 10;

        private void setLimit(int limit) {
            FinalService.limit = limit;
        }

        @Inject
        public FinalService(EventContext eventContext) {
            this.eventContext = eventContext;
        }

        public void reset() {
            count.set(0);
            FinalService.limit = 10;
            handledEvent = new CompletableFuture<>();
        }

        @MessageHandler
        public void handle(FinalServiceEvent event) {

            AMQP.BasicProperties amqpBasicProperties = this.eventContext.getAmqpBasicProperties();
            if (event.name().equalsIgnoreCase(amqpBasicProperties.getCorrelationId())) {
                if (count.incrementAndGet() == FinalService.limit) {
                    handledEvent.complete(event);
                }
            }
        }

        public CompletableFuture<FinalServiceEvent> getHandledEvent() {
            return handledEvent;
        }

    }

    public record Event(String name, Long age) {
    }

    @ConsumedEvent(queue = EVENT_QUEUE1, exchange = EXCHANGE)
    public record Service1Event(String name, Long age) {
    }

    @ConsumedEvent(queue = EVENT_QUEUE2, exchange = EXCHANGE)
    public record Service2Event(String name, Long age) {
    }

    @ConsumedEvent(queue = EVENT_QUEUE3, exchange = EXCHANGE)
    public record FinalServiceEvent(String name, Long age) {
    }

    @ProducedEvent(exchange = EXCHANGE, queue = EVENT_QUEUE1)
    private record AnnotatedEvent1(String name, long age) {
    }

    private void publishEvent(final String name, final String correlationId) {
        final MetadataInfo metadataInfo = new MetadataInfo(correlationId);
        CompletableFuture.runAsync(() -> {
            try {
                testProducer.send(
                        new Event(name, 0L),
                        EXCHANGE,
                        EVENT_QUEUE1,
                        DIRECT,
                        metadataInfo);
            } catch (AmqpSendException | IOException e) {
                fail();
            }
        });
    }

    private void publishEvent(final String name) {
        CompletableFuture.runAsync(() -> {
            try {
                testProducer.send(
                        new Event(name, 0L),
                        EXCHANGE,
                        EVENT_QUEUE1,
                        DIRECT);
            } catch (AmqpSendException | IOException e) {
                fail();
            }
        });
    }
}
