package org.iris_events.it;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.iris_events.annotations.ExchangeType.DIRECT;
import static org.iris_events.common.MessagingHeaders.Message.CURRENT_SERVICE_ID;
import static org.iris_events.common.MessagingHeaders.Message.EVENT_TYPE;
import static org.iris_events.common.MessagingHeaders.Message.INSTANCE_ID;
import static org.iris_events.common.MessagingHeaders.Message.ORIGIN_SERVICE_ID;
import static org.iris_events.common.MessagingHeaders.Message.SERVER_TIMESTAMP;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.iris_events.annotations.Message;
import org.iris_events.annotations.MessageHandler;
import org.iris_events.context.EventContext;
import org.iris_events.producer.EventProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.rabbitmq.client.AMQP;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventAppRecorderIT extends IsolatedEventContextTest {

    private static final String EVENT_QUEUE = "event-queue";
    private static final String EXCHANGE = "event-app/recorder-exchange";
    public static final String APP_ID = "test-app";

    @Inject
    EventProducer producer;

    @Inject
    Service service;

    @Test
    @DisplayName("Event published should be accompanied with custom event app info headers")
    void sendPropagatesCustomEventAppHeaders() throws Exception {

        producer.send(new Event());

        final var basicProperties = service.getEventContext().get(5, TimeUnit.SECONDS);

        final var headers = basicProperties.getHeaders();
        assertThat(headers.keySet(),
                containsInAnyOrder(
                        ORIGIN_SERVICE_ID,
                        CURRENT_SERVICE_ID,
                        INSTANCE_ID,
                        EVENT_TYPE,
                        SERVER_TIMESTAMP));
        assertThat(headers.get(ORIGIN_SERVICE_ID).toString(), is(APP_ID));
        assertThat(headers.get(CURRENT_SERVICE_ID).toString(), is(APP_ID));
        assertThat(headers.get(EVENT_TYPE).toString(), is(EXCHANGE));
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class Service {
        private CompletableFuture<AMQP.BasicProperties> basicPropertiesCompletableFuture = new CompletableFuture<>();
        private final EventContext eventContext;

        @Inject
        public Service(EventContext eventContext) {
            this.eventContext = eventContext;
        }

        @MessageHandler(bindingKeys = EVENT_QUEUE)
        public void handle(Event event) {
            final var amqpBasicProperties = this.eventContext.getAmqpBasicProperties();
            basicPropertiesCompletableFuture.complete(amqpBasicProperties);
        }

        public void reset() {
            basicPropertiesCompletableFuture = new CompletableFuture<>();
        }

        public CompletableFuture<AMQP.BasicProperties> getEventContext() {
            return basicPropertiesCompletableFuture;
        }
    }

    @Message(routingKey = EVENT_QUEUE, name = EXCHANGE, exchangeType = DIRECT)
    public record Event() {
    }
}
