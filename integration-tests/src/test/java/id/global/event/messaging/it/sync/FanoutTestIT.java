package id.global.event.messaging.it.sync;

import static id.global.common.annotations.amqp.ExchangeType.FANOUT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.MessageHandler;
import id.global.event.messaging.runtime.exception.AmqpSendException;
import id.global.event.messaging.runtime.exception.AmqpTransactionException;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
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

        FanoutLoggingEvent event = new FanoutLoggingEvent("INFO: 1337", 1L);
        producer.send(event);

        assertThat(loggingServiceA.getFuture().get(5, TimeUnit.SECONDS), samePropertyValuesAs(event));
        assertThat(loggingServiceB.getFuture().get(5, TimeUnit.SECONDS), samePropertyValuesAs(event));
    }

    @Test
    @DisplayName("Event published to FANOUT; one service all consumers should receive event")
    void publishFanoutOneService()
            throws ExecutionException, InterruptedException, AmqpSendException, AmqpTransactionException, TimeoutException {
        FanoutEvent event = new FanoutEvent("Fanout Event", 23L);

        producer.send(event);

        CompletableFuture.allOf(fanoutService.getFanout1(), fanoutService.getFanout2())
                .join();

        assertThat(fanoutService.getFanout1().get(5, TimeUnit.SECONDS), samePropertyValuesAs(event));
        assertThat(fanoutService.getFanout2().get(5, TimeUnit.SECONDS), samePropertyValuesAs(event));

        assertThat(fanoutService.getFanoutCount(), is(2));
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class LoggingServiceA {
        private final CompletableFuture<FanoutLoggingEvent> future = new CompletableFuture<>();

        @MessageHandler
        public void handleLogEvents(FanoutLoggingEvent event) {
            future.complete(event);
        }

        public CompletableFuture<FanoutLoggingEvent> getFuture() {
            return future;
        }
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class LoggingServiceB {
        private final CompletableFuture<FanoutLoggingEvent> future = new CompletableFuture<>();

        @MessageHandler
        public void handleLogEvents(FanoutLoggingEvent event) {
            future.complete(event);
        }

        public CompletableFuture<FanoutLoggingEvent> getFuture() {
            return future;
        }
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class FanoutService {

        private CompletableFuture<FanoutEvent> fanout1 = new CompletableFuture<>();
        private CompletableFuture<FanoutEvent> fanout2 = new CompletableFuture<>();

        public void reset() {
            fanout1 = new CompletableFuture<>();
            fanout2 = new CompletableFuture<>();
        }

        public CompletableFuture<FanoutEvent> getFanout1() {
            return fanout1;
        }

        public CompletableFuture<FanoutEvent> getFanout2() {
            return fanout2;
        }

        private final AtomicInteger eventCount = new AtomicInteger();

        public int getFanoutCount() {
            return eventCount.get();
        }

        @MessageHandler
        public void handleFanoutMessage(FanoutEvent event) {
            eventCount.getAndIncrement();
            fanout1.complete(event);
        }

        @MessageHandler
        public void handleFanoutOtherMessage(FanoutEvent event) {
            eventCount.getAndIncrement();
            fanout2.complete(event);
        }

    }

    @Message(name = EXCHANGE, exchangeType = FANOUT)
    public record FanoutLoggingEvent(String log, Long level) {
    }

    @Message(name = MY_FANOUT_EXCHANGE, exchangeType = FANOUT)
    public record FanoutEvent(String log, Long level) {
    }

}
