package id.global.iris.messaging.it.sync;

import static id.global.common.iris.annotations.ExchangeType.TOPIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import id.global.common.iris.annotations.Message;
import id.global.common.iris.annotations.MessageHandler;
import id.global.iris.messaging.it.IsolatedEventContextTest;
import id.global.iris.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TopicTestIT extends IsolatedEventContextTest {
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

        internalLoggingServiceA.getCompletionSignal().get(5, TimeUnit.SECONDS);
        internalLoggingServiceB.getCompletionSignal().get(5, TimeUnit.SECONDS);

        assertThat(internalLoggingServiceA.getEvents(),
                contains("Quick orange fox", "Lazy orange rabbit"));
        assertThat(internalLoggingServiceB.getEvents(),
                contains("Quick yellow rabbit", "Lazy blue snail", "Lazy orange rabbit"));
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class MyLoggingServiceA {
        public CompletableFuture<String> completionSignal = new CompletableFuture<>();

        private final List<String> events = new ArrayList<>();

        public List<String> getEvents() {
            return events;
        }

        public void reset() {
            events.clear();
            completionSignal = new CompletableFuture<>();
        }

        @MessageHandler(bindingKeys = { "*.orange.*" })
        public void handleLogEvents(ServiceALoggingEvent1 event) {
            synchronized (events) {
                events.add(event.log());
                if (events.size() == 2) {
                    completionSignal.complete("done");
                }
            }
        }

        public CompletableFuture<String> getCompletionSignal() {
            return completionSignal;
        }
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class MyLoggingServiceB {
        public CompletableFuture<String> completionSignal = new CompletableFuture<>();
        private final List<String> events = new ArrayList<>();

        public List<String> getEvents() {
            return events;
        }

        public void reset() {
            events.clear();
            completionSignal = new CompletableFuture<>();
        }

        @MessageHandler(bindingKeys = { "*.*.rabbit", "lazy.#" })
        public void handleLogEvents(ServiceBLoggingEvent event) {
            synchronized (events) {
                events.add(event.log());
                if (events.size() == 3) {
                    completionSignal.complete("done");
                }
            }
        }

        public CompletableFuture<String> getCompletionSignal() {
            return completionSignal;
        }
    }

    @Message(name = TOPIC_EXCHANGE, exchangeType = TOPIC, routingKey = "quick.orange.fox")
    public record ServiceALoggingEvent1(String log, Long level) {
    }

    @Message(name = TOPIC_EXCHANGE, exchangeType = TOPIC, routingKey = "quick.yellow.rabbit")
    public record ServiceALoggingEvent2(String log, Long level) {
    }

    @Message(name = TOPIC_EXCHANGE, exchangeType = TOPIC, routingKey = "lazy.blue.snail")
    public record ServiceALoggingEvent3(String log, Long level) {
    }

    @Message(name = TOPIC_EXCHANGE, exchangeType = TOPIC, routingKey = "lazy.orange.rabbit")
    public record ServiceALoggingEvent4(String log, Long level) {
    }

    @Message(name = TOPIC_EXCHANGE, exchangeType = TOPIC)
    public record ServiceBLoggingEvent(String log, Long level) {
    }
}
