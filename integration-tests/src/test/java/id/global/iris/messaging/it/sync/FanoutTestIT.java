package id.global.iris.messaging.it.sync;

import static id.global.common.annotations.iris.ExchangeType.FANOUT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import id.global.common.annotations.iris.Message;
import id.global.common.annotations.iris.MessageHandler;
import id.global.iris.messaging.it.IsolatedEventContextTest;
import id.global.iris.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;

/**
 * We are unable to test multiple consumers within the same app since this is invalid case.
 * All declared consumers for the same event would bind to the same queue.
 * <p>
 * Similarly, we are unable to test consumer per instance since all instances would be given same id on the same host.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FanoutTestIT extends IsolatedEventContextTest {

    private static final String EXCHANGE = "test-fanout-exchange";

    @Inject
    AmqpProducer producer;

    @Inject
    LoggingService loggingService;

    @Test
    @DisplayName("Event published to FANOUT")
    void publishFanout() throws Exception {

        FanoutLoggingEvent event = new FanoutLoggingEvent("INFO: 1337", 1L);
        producer.send(event);

        assertThat(loggingService.getFuture().get(5, TimeUnit.SECONDS), samePropertyValuesAs(event));
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class LoggingService {
        private final CompletableFuture<FanoutLoggingEvent> future = new CompletableFuture<>();

        @MessageHandler
        public void handleLogEvents(FanoutLoggingEvent event) {
            future.complete(event);
        }

        public CompletableFuture<FanoutLoggingEvent> getFuture() {
            return future;
        }
    }

    @Message(name = EXCHANGE, exchangeType = FANOUT)
    public record FanoutLoggingEvent(String log, Long level) {
    }
}
