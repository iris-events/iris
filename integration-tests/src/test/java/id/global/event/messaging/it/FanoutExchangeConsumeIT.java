package id.global.event.messaging.it;

import id.global.asyncapi.spec.annotations.FanoutMessageHandler;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.event.messaging.it.events.Event;
import id.global.event.messaging.it.events.LoggingEvent;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FanoutExchangeConsumeIT {
    @Inject AmqpProducer producer;

    @Inject MyLoggingServiceA internalLoggingServiceA;

    @Inject MyLoggingServiceB internalLoggingServiceB;

    @Inject TestHandlerService service;

    @BeforeEach
    public void setup() {
        service.reset();
        producer.connect();
    }

    private static final String EXCHANGE = "test_fanout_exchange";

    @Test
    void fanoutTest() throws Exception {
        producer.publishFanout(EXCHANGE,new LoggingEvent("this is log", 1L),null);

        assertEquals("this is log", internalLoggingServiceA.getFuture().get());
        assertEquals("this is log", internalLoggingServiceB.getFuture().get());
    }

    @Test
    void publishMessageToUnknownExchange_ShoutFail() throws Exception {
        producer.publishExchange("not known", new Event("a", 10L), null);

        while (!producer.isShutdown()){} //TODO: this is no OK, figure it out how to properly wait for shutdown
        System.out.println(service.getFanoutCount());

        assertTrue(producer.isShutdown());
        assertThrows(Exception.class, () -> producer.publishExchange("not known", new Event("a", 6L), null));
    }

    @Test
    void publishMessageToFanout_ShouldReceiveTwoMessages() throws Exception {
        producer.publishFanout("my.fanout", new Event("a", 23L), null);

        CompletableFuture.allOf(service.getFanout1(),service.getFanout2()).join();

        assertEquals(2, service.getFanoutCount());
    }

    @ApplicationScoped
    public static class MyLoggingServiceA {
        public CompletableFuture<String> future = new CompletableFuture<>();

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
        public CompletableFuture<String> future = new CompletableFuture<>();

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
        private  CompletableFuture<Event> fanout2 = new CompletableFuture<>();

        public void reset(){
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

        @FanoutMessageHandler(exchange = "my.fanout")
        public void handleFanoutMessage(Event event) {
            eventCount.getAndIncrement();
            fanout1.complete(event);
        }

        @FanoutMessageHandler(exchange = "my.fanout")
        public void handleFanoutOtherMessage(Event event) {
            eventCount.getAndIncrement();
            fanout2.complete(event);
        }

    }

}
