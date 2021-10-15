package id.global.event.messaging.it.sync;

import static id.global.asyncapi.spec.enums.ExchangeType.FANOUT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import id.global.asyncapi.spec.annotations.FanoutMessageHandler;
import id.global.event.messaging.it.events.Event;
import id.global.event.messaging.it.events.LoggingEvent;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FanoutTestIT {

    private static final String EXCHANGE = "test-fanout-exchange";
    private static final String MY_FANOUT_EXCHANGE = "my-fanout";

    @Inject
    AmqpProducer producer;

    @Inject
    LoggingServiceA loggingServiceA;

    @Inject
    LoggingServiceB loggingServiceB;

    @Inject
    FanoutService fanoutService;

    @BeforeEach
    public void setup() {
        fanoutService.reset();
    }

    @Test
    @DisplayName("Event published to FANOUT; two services all consumers should receive event")
    void publishFanout() throws Exception {

        LoggingEvent event = new LoggingEvent("INFO: 1337", 1L);
        producer.publish(event, EXCHANGE, "", FANOUT);

        assertThat(loggingServiceA.getFuture().get(), samePropertyValuesAs(event));
        assertThat(loggingServiceB.getFuture().get(), samePropertyValuesAs(event));
    }

    @Test
    @DisplayName("Event published to FANOUT; one service all consumers should receive event")
    void publishFanoutOneService()
            throws ExecutionException, InterruptedException {
        Event event = new Event("Fanout Event", 23L);

        producer.publish(event, MY_FANOUT_EXCHANGE, "", FANOUT);

        CompletableFuture.allOf(fanoutService.getFanout1(), fanoutService.getFanout2())
                .join();

        assertThat(fanoutService.getFanout1().get(), samePropertyValuesAs(event));
        assertThat(fanoutService.getFanout2().get(), samePropertyValuesAs(event));

        assertThat(fanoutService.getFanoutCount(), is(2));
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class LoggingServiceA {
        private final CompletableFuture<LoggingEvent> future = new CompletableFuture<>();

        @FanoutMessageHandler(exchange = EXCHANGE)
        public void handleLogEvents(LoggingEvent event) {
            future.complete(event);
        }

        public CompletableFuture<LoggingEvent> getFuture() {
            return future;
        }
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class LoggingServiceB {
        private final CompletableFuture<LoggingEvent> future = new CompletableFuture<>();

        @FanoutMessageHandler(exchange = EXCHANGE)
        public void handleLogEvents(LoggingEvent event) {
            future.complete(event);
        }

        public CompletableFuture<LoggingEvent> getFuture() {
            return future;
        }
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class FanoutService {

        private CompletableFuture<Event> fanout1 = new CompletableFuture<>();
        private CompletableFuture<Event> fanout2 = new CompletableFuture<>();

        public void reset() {
            fanout1 = new CompletableFuture<>();
            fanout2 = new CompletableFuture<>();
        }

        public CompletableFuture<Event> getFanout1() {
            return fanout1;
        }

        public CompletableFuture<Event> getFanout2() {
            return fanout2;
        }

        private final AtomicInteger eventCount = new AtomicInteger();

        public int getFanoutCount() {
            return eventCount.get();
        }

        @FanoutMessageHandler(exchange = MY_FANOUT_EXCHANGE)
        public void handleFanoutMessage(Event event) {
            eventCount.getAndIncrement();
            fanout1.complete(event);
        }

        @FanoutMessageHandler(exchange = MY_FANOUT_EXCHANGE)
        public void handleFanoutOtherMessage(Event event) {
            eventCount.getAndIncrement();
            fanout2.complete(event);
        }

    }

}
