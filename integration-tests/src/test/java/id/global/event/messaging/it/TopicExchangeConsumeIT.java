package id.global.event.messaging.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.global.asyncapi.spec.annotations.TopicMessageHandler;
import id.global.event.messaging.it.events.LoggingEvent;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
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
    }

    @Test
    void topicTest() throws Exception {
        LoggingEvent l1 = new LoggingEvent("Quick orange fox", 1L);
        LoggingEvent l2 = new LoggingEvent("Quick yellow rabbit", 2L);
        LoggingEvent l3 = new LoggingEvent("Lazy blue snail", 3L);
        LoggingEvent l4 = new LoggingEvent("Lazy orange rabbit", 4L);

        producer.publishTopicAsync(TOPIC_EXCHANGE, "quick.orange.fox", l1, null);
        producer.publishTopicAsync(TOPIC_EXCHANGE, "quick.yellow.rabbit", l2, null);
        producer.publishTopicAsync(TOPIC_EXCHANGE, "lazy.blue.snail", l3, null);
        producer.publishTopicAsync(TOPIC_EXCHANGE, "lazy.orange.rabbit", l4, null);

        Thread.sleep(150); //wait for little bit

        assertTrue(internalLoggingServiceA.getEvents().contains("Quick orange fox"));
        assertTrue(internalLoggingServiceB.getEvents().contains("Quick yellow rabbit"));
        assertTrue(internalLoggingServiceB.getEvents().contains("Lazy blue snail"));
        assertTrue(internalLoggingServiceA.getEvents().contains("Lazy orange rabbit"));
        assertTrue(internalLoggingServiceB.getEvents().contains("Lazy orange rabbit"));
    }

    @ApplicationScoped
    public static class MyLoggingServiceA {
        public List<CompletableFuture<String>> futureList = new ArrayList<>();
        public int i = 0;
        private List<String> events = new ArrayList<>();

        public List<String> getEvents() {
            return events;
        }

        public MyLoggingServiceA() {
            futureList.add(new CompletableFuture<>());
        }

        @TopicMessageHandler(exchange = TOPIC_EXCHANGE, bindingKeys = { "*.orange.*" })
        public void handleLogEvents(LoggingEvent event) {

            futureList.add(
                    CompletableFuture.supplyAsync(() -> {
                        events.add(event.getLog());
                        return event.getLog();
                    }));
        }

        public List<CompletableFuture<String>> getFutureList() {
            return futureList;
        }
    }

    @ApplicationScoped
    public static class MyLoggingServiceB {
        public List<CompletableFuture<String>> futureList = new ArrayList<>();
        public int i = 0;
        private List<String> events = new ArrayList<>();

        public List<String> getEvents() {
            return events;
        }

        public MyLoggingServiceB() {
            futureList.add(new CompletableFuture<>());
        }

        @TopicMessageHandler(exchange = TOPIC_EXCHANGE, bindingKeys = { "*.*.rabbit", "lazy.#" })
        public void handleLogEvents(LoggingEvent event) {
            futureList.add(
                    CompletableFuture.supplyAsync(() -> {
                        events.add(event.getLog());
                        return event.getLog();
                    }));
        }

        public List<CompletableFuture<String>> getFutureList() {
            return futureList;
        }
    }

}
