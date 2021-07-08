package id.global.event.messaging.it;

import id.global.asyncapi.spec.annotations.TopicMessageHandler;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class TopicExchangeConsumeIT {
    public static final String TOPIC_EXCHANGE = "topic_test_2222";

    @Inject AmqpProducer producer;

    @Inject MyLoggingServiceA internalLoggingServiceA;

    @Inject MyLoggingServiceB internalLoggingServiceB;

    @Test
    void topicTest() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        producer.declareTopicExchange(TOPIC_EXCHANGE);
        producer.sendMessage(new LoggingEvent("Quick orange fox", 1L), TOPIC_EXCHANGE, "quick.orange.fox");
        producer.sendMessage(new LoggingEvent("Quick yellow rabbit", 2L), TOPIC_EXCHANGE, "quick.yellow.rabbit");
        producer.sendMessage(new LoggingEvent("Lazy blue snail", 3L), TOPIC_EXCHANGE, "lazy.blue.snail");
        producer.sendMessage(new LoggingEvent("Lazy orange rabbit", 4L), TOPIC_EXCHANGE, "lazy.orange.rabbit");

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

    public static class LoggingEvent {

        private String log;
        private Long level;

        public LoggingEvent() {
        }

        public LoggingEvent(String log, Long level) {
            this.log = log;
            this.level = level;
        }

        public String getLog() {
            return log;
        }

        public Long getLevel() {
            return level;
        }

        public void setLog(String log) {
            this.log = log;
        }

        public void setLevel(Long level) {
            this.level = level;
        }
    }
}
