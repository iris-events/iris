package id.global.event.messaging.it;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.global.asyncapi.spec.annotations.TopicMessageHandler;
import id.global.event.messaging.it.events.LoggingEvent;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        producer.connect();
    }

    @Test
    void topicTest() throws Exception {
        producer.publishTopic(TOPIC_EXCHANGE, "quick.orange.fox", new LoggingEvent("Quick orange fox", 1L), null);
        producer.publishTopic(TOPIC_EXCHANGE, "quick.yellow.rabbit", new LoggingEvent("Quick yellow rabbit", 2L), null);
        producer.publishTopic(TOPIC_EXCHANGE, "lazy.blue.snail", new LoggingEvent("Lazy blue snail", 3L), null);
        producer.publishTopic(TOPIC_EXCHANGE, "lazy.orange.rabbit", new LoggingEvent("Lazy orange rabbit", 4L), null);

        assertEquals("Quick orange fox", internalLoggingServiceA.getFutureList().get(0).get(1, TimeUnit.SECONDS));
        assertEquals("Quick yellow rabbit", internalLoggingServiceB.getFutureList().get(0).get(1, TimeUnit.SECONDS));
        assertEquals("Lazy blue snail", internalLoggingServiceB.getFutureList().get(1).get(1, TimeUnit.SECONDS));
        assertEquals("Lazy orange rabbit", internalLoggingServiceA.getFutureList().get(1).get(1, TimeUnit.SECONDS));
        assertEquals("Lazy orange rabbit", internalLoggingServiceB.getFutureList().get(2).get(1, TimeUnit.SECONDS));
    }

    @ApplicationScoped
    public static class MyLoggingServiceA {
        public List<CompletableFuture<String>> futureList = new ArrayList<>();
        public int i = 0;

        public MyLoggingServiceA() {
            futureList.add(new CompletableFuture<>());
        }

        @TopicMessageHandler(exchange = TOPIC_EXCHANGE, bindingKeys = { "*.orange.*" })
        public void handleLogEvents(LoggingEvent event) {
            futureList.add(new CompletableFuture<>());
            futureList.get(i).complete(event.getLog());
            i++;
        }

        public List<CompletableFuture<String>> getFutureList() {
            return futureList;
        }
    }

    @ApplicationScoped
    public static class MyLoggingServiceB {
        public List<CompletableFuture<String>> futureList = new ArrayList<>();
        public int i = 0;

        public MyLoggingServiceB() {
            futureList.add(new CompletableFuture<>());
        }

        @TopicMessageHandler(exchange = TOPIC_EXCHANGE, bindingKeys = { "*.*.rabbit", "lazy.#" })
        public void handleLogEvents(LoggingEvent event) {
            futureList.add(new CompletableFuture<>());
            futureList.get(i).complete(event.getLog());
            i++;
        }

        public List<CompletableFuture<String>> getFutureList() {
            return futureList;
        }
    }

}
