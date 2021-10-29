package id.global.event.messaging.it.sync;

import static id.global.asyncapi.spec.enums.ExchangeType.DIRECT;
import static id.global.asyncapi.spec.enums.ExchangeType.FANOUT;
import static id.global.asyncapi.spec.enums.ExchangeType.TOPIC;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import id.global.asyncapi.spec.annotations.ConsumedEvent;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.annotations.ProducedEvent;
import id.global.asyncapi.spec.enums.ExchangeType;
import id.global.event.messaging.runtime.exception.AmqpSendException;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AnnotatedEventTestOverrideIT {
    /*
     * TODO this test should also be converted to a unit test where it tests only the overrides on the AmqpProducer send
     * method
     *
     * same as for AnnotatedEventsTestIT, refactoring of AmqpProducer (decoupling of rabbitmq stuff) is needed.
     */

    private static final String OVERRIDE_ANNOTATED_QUEUE = "override-annotated-queue";
    private static final String OVERRIDE_ANNOTATED_QUEUE_FANOUT = "override-annotated-queue-fanout";
    private static final String OVERRIDE_ANNOTATED_EXCHANGE = "override-annotated-exchange";
    private static final String OVERRIDE_ANNOTATED_EXCHANGE_FANOUT = "override-annotated-exchange-fanout";
    private static final String OVERRIDE_TOPIC_EXCHANGE = "override-topic-exchange";
    private static final String OVERRIDE_SOMETHING_NOTHING = "#.override-nothing";
    private static final String OVERRIDE_SOMETHING = "override-something.#";
    private static final String OVERRIDE_TOPIC1 = "nothing.a.override-nothing";
    private static final String OVERRIDE_TOPIC2 = "override-something.a.b";
    private static final String OVERRIDE_TOPIC3 = "override-something.a.everything";
    private static final String ANNOTATED_EXCHANGE = "annotated-exchange";
    private static final String ANNOTATED_QUEUE = "annotated-queue";
    private static final String TOPIC_EXCHANGE = "topic-exchange";
    private static final String ANNOTATED_EXCHANGE_FANOUT = "annotated-exchange-fanout";
    private static final String ANNOTATED_QUEUE_FANOUT = "annotated-queue-fanout";
    private static final String EMPTY = "";
    private static final String NAME = "name";
    private static final String DOESNT_MATTER = "Doesnt matter";
    private static final long AGE_1 = 1L;

    @Inject
    AmqpProducer testProducer;

    @Inject
    AnnotationService annotationService;

    @Inject
    TopicService topicService;

    @BeforeEach
    public void setUp() {
        annotationService.reset();
        topicService.reset();
    }

    private static Stream<Arguments> publishTypes() {
        return Stream.of(
                Arguments.of(new DirectEventEmptyExchange(), EMPTY, OVERRIDE_ANNOTATED_QUEUE, DIRECT, false),
                Arguments.of(new DirectEventEmptyRoutingKey(), OVERRIDE_ANNOTATED_EXCHANGE, EMPTY, DIRECT, false),
                Arguments.of(new FanoutEvent(), OVERRIDE_ANNOTATED_EXCHANGE_FANOUT, DOESNT_MATTER, FANOUT, true),
                Arguments.of(new FanoutEventWrongEmptyExchange(), EMPTY, OVERRIDE_ANNOTATED_QUEUE_FANOUT, FANOUT, false),
                Arguments.of(new FanoutEventWrongRoutingKey(), OVERRIDE_ANNOTATED_EXCHANGE_FANOUT, DOESNT_MATTER, FANOUT,
                        true));
    }

    @ParameterizedTest
    @MethodSource("publishTypes")
    @DisplayName("Published annotated event without exchange name to DIRECT exchange with overrides should fail")
    public void publishTest(Object event, String exchange, String queue, ExchangeType exchangeType, boolean shouldPublish) {
        if (shouldPublish) {
            assertDoesNotThrow(() -> testProducer.send(event, exchange, queue, exchangeType));
        } else {
            assertThrows(AmqpSendException.class, () -> testProducer.send(event, exchange, queue, exchangeType));
        }
    }

    @Test
    @DisplayName("Published correctly annotated event to DIRECT exchange with overrides should succeed")
    public void publishDirect() throws Exception {
        assertDoesNotThrow(() -> testProducer.send(new DirectEvent(NAME, AGE_1),
                OVERRIDE_ANNOTATED_EXCHANGE,
                OVERRIDE_ANNOTATED_QUEUE,
                DIRECT));
        annotationService.getCompletionSignal().get(2, TimeUnit.SECONDS);
        assertThat(annotationService.getCount(), is(1));
    }

    @Test
    @DisplayName("Published correctly annotated events to TOPIC exchange with overrides should succeed")
    public void publishTopic() throws ExecutionException, InterruptedException, TimeoutException {
        assertDoesNotThrow(() -> {
            testProducer.send(new TopicEventOne("name_one", AGE_1), OVERRIDE_TOPIC_EXCHANGE,
                    OVERRIDE_TOPIC1,
                    TOPIC); //will not be received
        });
        assertDoesNotThrow(() -> {
            testProducer.send(new TopicEventOne("name_two", AGE_1), OVERRIDE_TOPIC_EXCHANGE,
                    OVERRIDE_TOPIC2,
                    TOPIC); //will be received once
        });
        assertDoesNotThrow(() -> {
            testProducer.send(new TopicEventThree("name_three", AGE_1), OVERRIDE_TOPIC_EXCHANGE,
                    OVERRIDE_TOPIC3,
                    TOPIC); //will be received twice
        });
        String firstEventContent = topicService.getCompletionSignalOne().get(2, TimeUnit.SECONDS);
        String thirdEventContent = topicService.getCompletionSignalTwo().get(2, TimeUnit.SECONDS);
        assertThat(firstEventContent, is("name_one"));
        assertThat(thirdEventContent, is("name_three"));
        assertThat(topicService.getCount(), is(3));
    }

    @Test
    @DisplayName("Published annotated event without exchange name to TOPIC exchange with overrides should fail")
    public void publishTopicMissingExchange() {
        assertThrows(AmqpSendException.class, () -> testProducer.send(new TopicEventWrongEmptyExchange(NAME, AGE_1)));
        assertThat(topicService.getCount(), is(0));
    }

    @Test
    @DisplayName("Published annotated event without routing/binding key to TOPIC exchange with overrides should fail")
    public void publishTopicMissingRoutingKey() {
        assertThrows(AmqpSendException.class, () -> testProducer.send(new TopicEventWrongRoutingKey(NAME, AGE_1)));
        assertThat(topicService.getCount(), is(0));
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class AnnotationService {

        private CompletableFuture<Event> completedSignal = new CompletableFuture<>();
        private final AtomicInteger count = new AtomicInteger(0);

        @Inject
        public AnnotationService() {
        }

        @MessageHandler
        public void handle(DirectEvent event) {
            count.incrementAndGet();
            completedSignal.complete(new Event("completed", AGE_1));
        }

        public CompletableFuture<Event> getCompletionSignal() {
            return completedSignal;
        }

        public void reset() {
            completedSignal = new CompletableFuture<>();
            count.set(0);
        }

        public int getCount() {
            return count.get();
        }

    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class FirstFanoutService {

        @MessageHandler
        public void handleLogEvents(FanoutLoggingEvent event) {
        }
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class SecondFanoutService {

        @MessageHandler
        public void handleLogEvents(FanoutLoggingEvent event) {
        }
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class TopicService {

        private CompletableFuture<String> completionSignalOne = new CompletableFuture<>();
        private CompletableFuture<String> completionSignalTwo = new CompletableFuture<>();
        private final AtomicInteger count = new AtomicInteger(0);

        private CountDownLatch countDownLatch = new CountDownLatch(99);

        @MessageHandler
        public void handleLogEventsOne(TopicReceivedEventOne event) {
            count.incrementAndGet();
            completionSignalOne.complete(event.name());
        }

        @MessageHandler
        public void handleLogEventsTwo(TopicReceivedEventTwo event) {
            count.incrementAndGet();
            countDownLatch.countDown();
            if ((countDownLatch.getCount()) == 0) {
                completionSignalTwo.complete(event.name());
            }
        }

        public CompletableFuture<String> getCompletionSignalOne() {
            return completionSignalOne;
        }

        public CompletableFuture<String> getCompletionSignalTwo() {
            return completionSignalTwo;
        }

        public void reset() {
            completionSignalOne = new CompletableFuture<>();
            completionSignalTwo = new CompletableFuture<>();
            count.set(0);
            countDownLatch = new CountDownLatch(2);
        }

        public int getCount() {
            return count.get();
        }

    }

    public record Event(String name, Long age) {
    }

    @ConsumedEvent(exchange = OVERRIDE_ANNOTATED_EXCHANGE_FANOUT, exchangeType = FANOUT)
    public record FanoutLoggingEvent(String log, Long level) {
    }

    @ConsumedEvent(exchange = OVERRIDE_TOPIC_EXCHANGE, exchangeType = ExchangeType.TOPIC, bindingKeys = OVERRIDE_SOMETHING_NOTHING)
    private record TopicReceivedEventOne(String name, long age) {
    }

    @ConsumedEvent(exchange = OVERRIDE_TOPIC_EXCHANGE, exchangeType = ExchangeType.TOPIC, bindingKeys = OVERRIDE_SOMETHING)
    private record TopicReceivedEventTwo(String name, long age) {
    }

    @ProducedEvent(exchange = ANNOTATED_EXCHANGE, queue = ANNOTATED_QUEUE)
    @ConsumedEvent(queue = OVERRIDE_ANNOTATED_QUEUE, exchange = OVERRIDE_ANNOTATED_EXCHANGE, exchangeType = DIRECT)
    private record DirectEvent(String name, Long age) {
    }

    @ProducedEvent(exchange = TOPIC_EXCHANGE, queue = "nothing.a.nothing", exchangeType = TOPIC)
    private record TopicEventOne(String name, Long age) {
    }

    @ProducedEvent(exchange = TOPIC_EXCHANGE, queue = "something.a.everything", exchangeType = TOPIC)
    private record TopicEventThree(String name, Long age) {
    }

    @ProducedEvent(exchange = ANNOTATED_EXCHANGE_FANOUT, queue = ANNOTATED_QUEUE_FANOUT, exchangeType = FANOUT)
    private record FanoutEventWrongRoutingKey() {
    }

    @ProducedEvent(queue = ANNOTATED_QUEUE)
    private record DirectEventEmptyExchange() {
    }

    @ProducedEvent(exchange = ANNOTATED_EXCHANGE, queue = EMPTY)
    private record DirectEventEmptyRoutingKey() {
    }

    @ProducedEvent(exchange = ANNOTATED_EXCHANGE_FANOUT, queue = EMPTY, exchangeType = FANOUT)
    private record FanoutEvent() {
    }

    @ProducedEvent(queue = ANNOTATED_QUEUE_FANOUT, exchangeType = FANOUT)
    private record FanoutEventWrongEmptyExchange() {
    }

    @ProducedEvent(queue = ANNOTATED_QUEUE, exchangeType = TOPIC)
    private record TopicEventWrongEmptyExchange(String name, Long age) {
    }

    @ProducedEvent(exchange = ANNOTATED_EXCHANGE, queue = EMPTY, exchangeType = TOPIC)
    private record TopicEventWrongRoutingKey(String name, Long age) {
    }

    private record ReceivedEvent(String name, long age) {
    }
}
