package id.global.event.messaging.it.sync;

import static id.global.asyncapi.spec.enums.ExchangeType.TOPIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import id.global.asyncapi.spec.annotations.ConsumedEvent;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.annotations.ProducedEvent;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TopicTestIT {
    private static final String TOPIC_EXCHANGE = "topic-test-2222";

    @Inject
    AmqpProducer producer;
    @Inject
    MyLoggingServiceA internalLoggingServiceA;

    @Inject
    MyLoggingServiceB internalLoggingServiceB;

    @BeforeEach
    public void setup() {
        internalLoggingServiceA.reset();
        internalLoggingServiceB.reset();
    }

    @Test
    @DisplayName("Publishing message to topic exchange should route correctly to all services")
    void publishTopic() throws Exception {

        //messages can be consumed in order

        ServiceALoggingEvent1 l1 = new ServiceALoggingEvent1("Quick orange fox", 1L);
        ServiceALoggingEvent2 l2 = new ServiceALoggingEvent2("Quick yellow rabbit", 2L);
        ServiceALoggingEvent3 l3 = new ServiceALoggingEvent3("Lazy blue snail", 3L);
        ServiceALoggingEvent4 l4 = new ServiceALoggingEvent4("Lazy orange rabbit", 4L);

        producer.send(l1);
        producer.send(l2);
        producer.send(l3);
        producer.send(l4);

        MyLoggingServiceA.completionSignal.get();
        MyLoggingServiceB.completionSignal.get();

        assertThat(internalLoggingServiceA.getEvents(),
                contains("Quick orange fox", "Lazy orange rabbit"));
        assertThat(internalLoggingServiceB.getEvents(),
                contains("Quick yellow rabbit", "Lazy blue snail", "Lazy orange rabbit"));
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class MyLoggingServiceA {
        public static CompletableFuture<String> completionSignal = new CompletableFuture<>();

        private final List<String> events = new ArrayList<>();

        public List<String> getEvents() {
            return events;
        }

        public void reset() {
            events.clear();
            completionSignal = new CompletableFuture<>();
        }

        @MessageHandler
        public void handleLogEvents(ServiceALoggingEvent1 event) {
            synchronized (events) {
                events.add(event.log());
                if (events.size() == 2) {
                    completionSignal.complete("done");
                }
            }
        }
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class MyLoggingServiceB {

        public static CompletableFuture<String> completionSignal = new CompletableFuture<>();
        private final List<String> events = new ArrayList<>();

        public List<String> getEvents() {
            return events;
        }

        public void reset() {
            events.clear();
            completionSignal = new CompletableFuture<>();
        }

        @MessageHandler
        public void handleLogEvents(ServiceBLoggingEvent event) {
            synchronized (events) {
                events.add(event.log());
                if (events.size() == 3) {
                    completionSignal.complete("done");
                }
            }
        }
    }

    @ProducedEvent(exchange = TOPIC_EXCHANGE, exchangeType = TOPIC, queue = "quick.orange.fox")
    @ConsumedEvent(exchange = TOPIC_EXCHANGE, exchangeType = TOPIC, bindingKeys = { "*.orange.*" })
    public record ServiceALoggingEvent1(String log, Long level) {
    }

    @ProducedEvent(exchange = TOPIC_EXCHANGE, exchangeType = TOPIC, queue = "quick.yellow.rabbit")
    @ConsumedEvent(exchange = TOPIC_EXCHANGE, exchangeType = TOPIC, bindingKeys = { "*.orange.*" })
    public record ServiceALoggingEvent2(String log, Long level) {
    }

    @ProducedEvent(exchange = TOPIC_EXCHANGE, exchangeType = TOPIC, queue = "lazy.blue.snail")
    @ConsumedEvent(exchange = TOPIC_EXCHANGE, exchangeType = TOPIC, bindingKeys = { "*.orange.*" })
    public record ServiceALoggingEvent3(String log, Long level) {
    }

    @ProducedEvent(exchange = TOPIC_EXCHANGE, exchangeType = TOPIC, queue = "lazy.orange.rabbit")
    @ConsumedEvent(exchange = TOPIC_EXCHANGE, exchangeType = TOPIC, bindingKeys = { "*.orange.*" })
    public record ServiceALoggingEvent4(String log, Long level) {
    }

    @ConsumedEvent(exchange = TOPIC_EXCHANGE, exchangeType = TOPIC, bindingKeys = { "*.*.rabbit", "lazy.#" })
    public record ServiceBLoggingEvent(String log, Long level) {
    }
}
