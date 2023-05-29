package org.iris_events.it.sync;

import static org.iris_events.annotations.ExchangeType.DIRECT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.iris_events.it.IsolatedEventContextTest;
import org.iris_events.producer.EventProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import org.iris_events.annotations.Message;
import org.iris_events.annotations.MessageHandler;
import org.iris_events.runtime.InstanceInfoProvider;
import org.iris_events.context.EventContext;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MessagePropagationIT extends IsolatedEventContextTest {

    private static final String INITIAL_CONSUMING_QUEUE = "initial-consuming-queue";
    private static final String FORWARDING_QUEUE = "forwarding-queue";
    private static final String EXCHANGE = "message-propagation-exchange";

    @Inject
    EventProducer producer;

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
