package id.global.event.messaging.it;

import static id.global.asyncapi.spec.enums.ExchangeType.DIRECT;
import static id.global.event.messaging.runtime.producer.AmqpProducer.HEADER_CURRENT_SERVICE_ID;
import static id.global.event.messaging.runtime.producer.AmqpProducer.HEADER_INSTANCE_ID;
import static id.global.event.messaging.runtime.producer.AmqpProducer.HEADER_ORIGIN_SERVICE_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.CompletableFuture;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.rabbitmq.client.AMQP;

import id.global.asyncapi.spec.annotations.ConsumedEvent;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.annotations.ProducedEvent;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.asyncapi.spec.annotations.EventApp;
import io.smallrye.asyncapi.spec.annotations.info.Info;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventAppRecorderIT {

    private static final String EVENT_QUEUE = "event-queue";
    private static final String EXCHANGE = "event-app-recorder-exchange";
    public static final String APP_ID = "test-quarkus-eda";
    public static final String APP_DESCRIPTION = "Quarkus EDA test application";
    public static final String APP_TITLE = "Quarkus EDA";

    @Inject
    AmqpProducer producer;

    @Inject
    Service service;

    @Test
    @DisplayName("Event published should be accompanied with custom event app info headers")
    void sendPropagatesCustomEventAppHeaders() throws Exception {

        producer.send(new Event());

        final var basicProperties = service.getEventContext().get();

        final var headers = basicProperties.getHeaders();
        assertThat(headers.keySet(),
                containsInAnyOrder(
                        HEADER_ORIGIN_SERVICE_ID,
                        HEADER_CURRENT_SERVICE_ID,
                        HEADER_INSTANCE_ID));

        assertThat(headers.get(HEADER_ORIGIN_SERVICE_ID).toString(), is(APP_ID));
        assertThat(headers.get(HEADER_CURRENT_SERVICE_ID).toString(), is(APP_ID));
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

        @MessageHandler
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

    @ConsumedEvent(queue = EVENT_QUEUE, exchange = EXCHANGE, exchangeType = DIRECT)
    @ProducedEvent(queue = EVENT_QUEUE, exchange = EXCHANGE, exchangeType = DIRECT)
    public record Event() {
    }

    @EventApp(id = APP_ID, info = @Info(description = APP_DESCRIPTION, title = APP_TITLE))
    public static class TestApplication extends Application {
    }

}
