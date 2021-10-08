package id.global.event.messaging.it.async;

import static id.global.asyncapi.spec.enums.ExchangeType.FANOUT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import id.global.asyncapi.spec.annotations.FanoutMessageHandler;
import id.global.event.messaging.it.events.Event;
import id.global.event.messaging.it.events.LoggingEvent;
import id.global.event.messaging.runtime.producer.AmqpAsyncProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FanoutExchangeConsumeAsyncIT {
    @Inject
    AmqpAsyncProducer producer;

    @Inject
    MyLoggingServiceA internalLoggingServiceA;

    @Inject
    MyLoggingServiceB internalLoggingServiceB;

    @Inject
    TestHandlerService service;

    @BeforeEach
    public void setup() {
        service.reset();
    }

    private static final String EXCHANGE = "test-fanout-exchange-async";
    private static final String EXCHANGE_SECOND = "my-fanout-async";

    @Test
    void fanoutTest() throws Exception {
        producer.publishAsync(EXCHANGE,
                Optional.empty(),
                FANOUT,
                new LoggingEvent("this is log", 1L));

        assertThat(internalLoggingServiceA.getFuture().get(), is("this is log"));
        assertThat(internalLoggingServiceB.getFuture().get(), is("this is log"));
    }

    @Test
    void publishMessageToFanout_ShouldReceiveTwoMessages() {
        producer.publishAsync(EXCHANGE_SECOND,
                Optional.empty(),
                FANOUT,
                new Event("a", 23L));

        CompletableFuture.allOf(service.getFanout1(), service.getFanout2()).join();

        assertThat(service.getFanoutCount(), is(2));
    }

    @ApplicationScoped
    public static class MyLoggingServiceA {
        private final CompletableFuture<String> future = new CompletableFuture<>();

        @FanoutMessageHandler(exchange = EXCHANGE)
        public void handleLogEvents(LoggingEvent event) {
            future.complete(event.getLog());
        }

        public CompletableFuture<String> getFuture() {
            return future;
        }
    }

    @ApplicationScoped
    public static class MyLoggingServiceB {
        private final CompletableFuture<String> future = new CompletableFuture<>();

        @FanoutMessageHandler(exchange = EXCHANGE)
        public void handleLogEvents(LoggingEvent event) {
            future.complete(event.getLog());
        }

        public CompletableFuture<String> getFuture() {
            return future;
        }
    }

    @ApplicationScoped
    public static class TestHandlerService {

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

        @FanoutMessageHandler(exchange = EXCHANGE_SECOND)
        public void handleFanoutMessage(Event event) {
            eventCount.getAndIncrement();
            fanout1.complete(event);
        }

        @FanoutMessageHandler(exchange = EXCHANGE_SECOND)
        public void handleFanoutOtherMessage(Event event) {
            eventCount.getAndIncrement();
            fanout2.complete(event);
        }

    }

}
