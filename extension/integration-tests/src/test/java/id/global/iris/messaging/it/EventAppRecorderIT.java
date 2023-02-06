package id.global.iris.messaging.it;

import static id.global.iris.common.annotations.ExchangeType.DIRECT;
import static id.global.iris.common.constants.MessagingHeaders.Message.CURRENT_SERVICE_ID;
import static id.global.iris.common.constants.MessagingHeaders.Message.EVENT_TYPE;
import static id.global.iris.common.constants.MessagingHeaders.Message.INSTANCE_ID;
import static id.global.iris.common.constants.MessagingHeaders.Message.ORIGIN_SERVICE_ID;
import static id.global.iris.common.constants.MessagingHeaders.Message.SERVER_TIMESTAMP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.rabbitmq.client.AMQP;

import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.MessageHandler;
import id.global.iris.messaging.runtime.context.EventContext;
import id.global.iris.messaging.runtime.producer.EventProducer;
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
