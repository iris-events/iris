package id.global.iris.messaging.it.sync;

import static id.global.iris.common.annotations.ExchangeType.DIRECT;
import static id.global.iris.common.annotations.ExchangeType.FANOUT;
import static id.global.iris.common.annotations.ExchangeType.TOPIC;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.MessageHandler;
import id.global.iris.messaging.it.IsolatedEventContextTest;
import id.global.iris.messaging.runtime.exception.IrisSendException;
import id.global.iris.messaging.runtime.producer.EventProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AnnotatedEventsTestIT extends IsolatedEventContextTest {
    // TODO this looks more like it should be a unit test for AmqpProducer.
    /*
     * That means only checking if the produce method returns expected results according to inputs (correctly/incorrectly
     * annotated
     * events, null/non-null message bodies, etc.
     *
     * That requires decoupling all the rabbit ConnectionFactory, Connection, Channel etc. logic from the AmqpProducer so it can
     * be easily mocked.
     *
     * Currently checking of annotationService results will be removed, as this will only test returns from amqpProducer send
     */

    private static final String ANNOTATED_QUEUE = "annotated-queue";
    private static final String ANNOTATED_EXCHANGE = "annotated-exchange";
    private static final String ANNOTATED_EXCHANGE_TOPIC = "annotated-exchange-topic";
    private static final String ANNOTATED_EXCHANGE_FANOUT = "annotated-exchange-fanout";
    private static final String TOPIC_EXCHANGE = "topic-exchange";
    private static final String SOMETHING_NOTHING = "#.nothing";
    private static final String SOMETHING = "something.#";
    private static final String ANNOTATED_QUEUE_FANOUT = "annotated-queue-fanout";

    @Inject
    EventProducer testProducer;

    @Inject
    AnnotationService annotationService;

    @Inject
    TopicService topicService;

    @BeforeEach
    public void setUp() {
        annotationService.reset();
        topicService.reset();
    }

    @Test
    @DisplayName("Event without annotations should not be published successfully.")
    void publishMessageWithoutAnnotations() {
        final var amqpSendException = assertThrows(IrisSendException.class, () -> {
            testProducer.send(new Event("name", 1L));
        });
        MatcherAssert.assertThat(amqpSendException.getMessage(), is("Message annotation is required."));
    }

    public record Event(String name, Long age) {
    }

    @Test
    @DisplayName("Published correctly annotated event to DIRECT exchange should succeed")
    void publishDirect() {
        DirectEvent publishedEvent = new DirectEvent("name", 1L);
        assertDoesNotThrow(() -> testProducer.send(publishedEvent));
    }

    @Test
    @DisplayName("Published correctly annotated event to FANOUT exchange should succeed")
    void publishFanout() {
        assertDoesNotThrow(() -> testProducer.send(new FanoutEvent("name", 1L)));
    }

    @Test
    @DisplayName("Published annotated event with routing key to FANOUT exchange should succeed")
    void publishFanoutWithRoutingKey() {
        // Publish should ignore routing key in case of FANOUT exchange
        // TODO check if there is a warning logged in this case
        assertDoesNotThrow(() -> testProducer.send(new FanoutEventWrongRoutingKey("name", 1L)));
    }

    @Test
    @DisplayName("Published correctly annotated events to TOPIC exchange should succeed")
    void publishTopic() {
        assertDoesNotThrow(() -> {
            testProducer.send(new TopicEventOne("name_one", 1L)); //will not be received
        });
        assertDoesNotThrow(() -> {
            testProducer.send(new TopicEventTwo("name_two", 1L)); //will be received once
        });
        assertDoesNotThrow(() -> {
            testProducer.send(new TopicEventThree("name_three", 1L)); //will be received twice
        });
    }

    @Test
    @DisplayName("Published annotated event without routing/binding key to TOPIC exchange should not fail")
    void publishTopicMissingRoutingKey() {
        assertDoesNotThrow(() -> {
            testProducer.send(new TopicEventWrongRoutingKey("name", 1L));
        });
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class AnnotationService {

        private CompletableFuture<DirectEvent> handledEvent = new CompletableFuture<>();
        private final AtomicInteger count = new AtomicInteger(0);

        @Inject
        public AnnotationService() {
        }

        @MessageHandler
        public void handle(DirectEvent event) {
            count.incrementAndGet();
            handledEvent.complete(event);
        }

        public CompletableFuture<DirectEvent> getHandledEvent() {
            return handledEvent;
        }

        public void reset() {
            handledEvent = new CompletableFuture<>();
            count.set(0);
        }

        public int getCount() {
            return count.get();
        }

    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class FanoutService {

        private final CompletableFuture<String> future = new CompletableFuture<>();

        @MessageHandler
        public void handleLogEvents(FanoutLoggingEvent event) {
            future.complete(event.log());
        }

        public CompletableFuture<String> getFuture() {
            return future;
        }

    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class TopicService {

        private CompletableFuture<String> futureOne = new CompletableFuture<>();
        private CompletableFuture<String> futureTwo = new CompletableFuture<>();
        private final AtomicInteger count = new AtomicInteger(0);

        private CountDownLatch countDownLatch = new CountDownLatch(99);

        @MessageHandler(bindingKeys = SOMETHING_NOTHING)
        public void handleLogEventsOne(TopicReceivedEventOne event) {
            count.incrementAndGet();
            futureOne.complete(event.name());
        }

        @MessageHandler(bindingKeys = SOMETHING)
        public void handleLogEventsTwo(TopicReceivedEventTwo event) {
            count.incrementAndGet();
            countDownLatch.countDown();
            if ((countDownLatch.getCount()) == 0) {
                futureTwo.complete(event.name());
            }
        }

        public CompletableFuture<String> getFutureOne() {
            return futureOne;
        }

        public CompletableFuture<String> getFutureTwo() {
            return futureTwo;
        }

        public void reset() {
            futureOne = new CompletableFuture<>();
            futureTwo = new CompletableFuture<>();
            count.set(0);
            countDownLatch = new CountDownLatch(2);
        }

        public int getCount() {
            return count.get();
        }

    }

    @Message(name = ANNOTATED_EXCHANGE_FANOUT, exchangeType = FANOUT)
    public record FanoutLoggingEvent(String log, Long level) {
    }

    @Message(name = TOPIC_EXCHANGE, exchangeType = TOPIC, routingKey = "something.nothing")
    private record TopicReceivedEventOne(String name, long age) {
    }

    @Message(name = TOPIC_EXCHANGE, exchangeType = TOPIC, routingKey = "something.dummy")
    private record TopicReceivedEventTwo(String name, long age) {
    }

    @Message(name = ANNOTATED_EXCHANGE, routingKey = ANNOTATED_QUEUE, exchangeType = DIRECT)
    private record DirectEvent(String name, Long age) {
    }

    @Message(name = ANNOTATED_EXCHANGE, exchangeType = DIRECT)
    private record DirectEventEmptyRoutingKey(String name, Long age) {
    }

    @Message(name = TOPIC_EXCHANGE, routingKey = "nothing.a.nothing", exchangeType = TOPIC)
    private record TopicEventOne(String name, Long age) {
    }

    @Message(name = TOPIC_EXCHANGE, routingKey = "something.a.b", exchangeType = TOPIC)
    private record TopicEventTwo(String name, Long age) {
    }

    @Message(name = TOPIC_EXCHANGE, routingKey = "something.a.everything", exchangeType = TOPIC)
    private record TopicEventThree(String name, Long age) {
    }

    @Message(name = ANNOTATED_EXCHANGE_FANOUT, routingKey = ANNOTATED_QUEUE_FANOUT, exchangeType = FANOUT)
    private record FanoutEventWrongRoutingKey(String name, Long age) {
    }

    @Message(name = ANNOTATED_EXCHANGE_FANOUT, exchangeType = FANOUT)
    private record FanoutEvent(String name, Long age) {
    }

    @Message(name = ANNOTATED_EXCHANGE_TOPIC, exchangeType = TOPIC)
    private record TopicEventWrongRoutingKey(String name, Long age) {
    }
}
