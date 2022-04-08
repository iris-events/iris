package id.global.iris.messaging.it.sync;

import static id.global.common.annotations.iris.ExchangeType.DIRECT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import id.global.common.annotations.iris.Message;
import id.global.common.annotations.iris.MessageHandler;
import id.global.iris.messaging.it.IsolatedEventContextTest;
import id.global.iris.messaging.runtime.InstanceInfoProvider;
import id.global.iris.messaging.runtime.context.EventContext;
import id.global.iris.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MessagePropagationIT extends IsolatedEventContextTest {

    private static final String INITIAL_CONSUMING_QUEUE = "initial-consuming-queue";
    private static final String FORWARDING_QUEUE = "forwarding-queue";
    private static final String EXCHANGE = "message-propagation-exchange";

    @Inject
    AmqpProducer producer;

    @Inject
    Service originService;

    @Inject
    ForwardedToService forwardedToService;

    @InjectMock
    InstanceInfoProvider instanceInfoProvider;

    @AfterEach
    void cleanup() {
        Mockito.reset(instanceInfoProvider);
    }

    @Test
    @DisplayName("Method handler return object should be forwarded to the annotated produced queue")
    void messageHandlerForwardsReturnedEvent() throws Exception {
        final var eventPropertyValue = UUID.randomUUID().toString();

        producer.send(new HandledEvent(eventPropertyValue));

        final var forwardedEvent = forwardedToService.getForwardedEvent().get(5, TimeUnit.SECONDS);
        assertThat(forwardedEvent, is(notNullValue()));
        assertThat(forwardedEvent.eventPropertyValue(), is(eventPropertyValue));
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class Service {

        @MessageHandler
        public ForwardedEvent handle(HandledEvent event) {
            return new ForwardedEvent(event.eventPropertyValue());
        }
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class ForwardedToService {
        private final CompletableFuture<ForwardedEvent> forwardedEventCompletableFuture = new CompletableFuture<>();
        private final EventContext eventContext;

        @Inject
        public ForwardedToService(EventContext eventContext) {
            this.eventContext = eventContext;
        }

        @MessageHandler
        public void handle(ForwardedEvent event) {
            final var amqpBasicProperties = this.eventContext.getAmqpBasicProperties();
            forwardedEventCompletableFuture.complete(event);
        }

        public CompletableFuture<ForwardedEvent> getForwardedEvent() {
            return forwardedEventCompletableFuture;
        }
    }

    @Message(routingKey = INITIAL_CONSUMING_QUEUE, name = EXCHANGE, exchangeType = DIRECT)
    public record HandledEvent(String eventPropertyValue) {
    }

    @Message(routingKey = FORWARDING_QUEUE, name = EXCHANGE, exchangeType = DIRECT)
    public record ForwardedEvent(String eventPropertyValue) {
    }
}
