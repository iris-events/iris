package id.global.event.messaging.it.sync;

import static id.global.asyncapi.spec.enums.ExchangeType.TOPIC;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import id.global.asyncapi.spec.annotations.TopicMessageHandler;
import id.global.event.messaging.it.events.LoggingEvent;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TopicExchangeConsumeIT {
    public static final String TOPIC_EXCHANGE = "topic_test_2222";

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
    void topicTest() throws Exception {

        //messages can be consumed in order

        LoggingEvent l1 = new LoggingEvent("Quick orange fox", 1L);
        LoggingEvent l2 = new LoggingEvent("Quick yellow rabbit", 2L);
        LoggingEvent l3 = new LoggingEvent("Lazy blue snail", 3L);
        LoggingEvent l4 = new LoggingEvent("Lazy orange rabbit", 4L);

        producer.publish(TOPIC_EXCHANGE, Optional.of("quick.orange.fox"), TOPIC, l1, true);
        producer.publish(TOPIC_EXCHANGE, Optional.of("quick.yellow.rabbit"), TOPIC, l2, true);
        producer.publish(TOPIC_EXCHANGE, Optional.of("lazy.blue.snail"), TOPIC, l3, true);
        producer.publish(TOPIC_EXCHANGE, Optional.of("lazy.orange.rabbit"), TOPIC, l4, true);

        MyLoggingServiceA.completionSignal.get();
        MyLoggingServiceB.completionSignal.get();

        assertTrue(internalLoggingServiceA.getEvents().get(0).contains("Quick orange fox"));
        assertTrue(internalLoggingServiceB.getEvents().get(0).contains("Quick yellow rabbit"));
        assertTrue(internalLoggingServiceB.getEvents().get(1).contains("Lazy blue snail"));
        assertTrue(internalLoggingServiceA.getEvents().get(1).contains("Lazy orange rabbit"));
        assertTrue(internalLoggingServiceB.getEvents().get(2).contains("Lazy orange rabbit"));
    }

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

        @TopicMessageHandler(exchange = TOPIC_EXCHANGE, bindingKeys = { "*.orange.*" })
        public void handleLogEvents(LoggingEvent event) {
            synchronized (events) {
                events.add(event.getLog());
                if (events.size() == 2) {
                    completionSignal.complete("done");
                }
            }
        }
    }

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

        @TopicMessageHandler(exchange = TOPIC_EXCHANGE, bindingKeys = { "*.*.rabbit", "lazy.#" })
        public void handleLogEvents(LoggingEvent event) {
            synchronized (events) {
                events.add(event.getLog());
                if (events.size() == 3) {
                    completionSignal.complete("done");
                }
            }
        }
    }

}