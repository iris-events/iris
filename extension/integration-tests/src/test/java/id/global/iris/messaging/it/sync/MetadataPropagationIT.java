package id.global.iris.messaging.it.sync;

import static id.global.iris.common.annotations.ExchangeType.DIRECT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import com.rabbitmq.client.AMQP;

import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.MessageHandler;
import id.global.iris.messaging.it.IsolatedEventContextTest;
import id.global.iris.messaging.runtime.InstanceInfoProvider;
import id.global.iris.messaging.runtime.context.EventContext;
import id.global.iris.messaging.runtime.exception.IrisSendException;
import id.global.iris.messaging.runtime.exception.IrisTransactionException;
import id.global.iris.messaging.runtime.producer.CorrelationIdProvider;
import id.global.iris.messaging.runtime.producer.EventProducer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetadataPropagationIT extends IsolatedEventContextTest {

    private static final String EVENT_QUEUE1 = "mpi-queue1";
    private static final String EVENT_QUEUE2 = "mpi-queue2";
    private static final String EVENT_QUEUE3 = "mpi-queue3";
    private static final String EXCHANGE = "metadata-propagation-it";

    @Inject
    EventProducer producer;

    @Inject
    Service1 service1;

    @Inject
    Service2 service2;

    @Inject
    FinalService finalService;

    @InjectMock
    CorrelationIdProvider correlationIdProvider;

    @InjectMock
    InstanceInfoProvider instanceInfoProvider;

    @AfterEach
    void cleanup() {
        finalService.reset();
        Mockito.reset(instanceInfoProvider);
    }

    @Test
    @DisplayName("Event published should be accompanied with correlationId to the final service")
    void publishPropagatesCorrelationId() throws Exception {
        final var correlationId = UUID.randomUUID().toString();

        Mockito.when(correlationIdProvider.getCorrelationId()).thenReturn(correlationId);

        producer.send(new Event1());

        final var basicProperties = finalService.getEventContext().get(5, TimeUnit.SECONDS);
        assertThat(basicProperties.getCorrelationId(), is(correlationId));
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class Service1 {
        private final EventProducer producer;

        @Inject
        public Service1(EventProducer producer) {
            this.producer = producer;
        }

        @MessageHandler(bindingKeys = EVENT_QUEUE1)
        public void handle(Event1 event) throws IrisSendException, IrisTransactionException {
            final var forwardedEvent = new Event2();
            producer.send(forwardedEvent);
        }
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class Service2 {
        private final EventProducer producer;

        @Inject
        public Service2(EventProducer producer) {
            this.producer = producer;
        }

        @MessageHandler(bindingKeys = EVENT_QUEUE2)
        public void handle(Event2 event) throws IrisSendException, IrisTransactionException {
            final var forwardedEvent = new Event3();
            producer.send(forwardedEvent);
        }
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class FinalService {
        private CompletableFuture<AMQP.BasicProperties> basicPropertiesCompletableFuture = new CompletableFuture<>();
        private final EventContext eventContext;

        @Inject
        public FinalService(EventContext eventContext) {
            this.eventContext = eventContext;
        }

        @MessageHandler(bindingKeys = EVENT_QUEUE3)
        public void handle(Event3 event) {
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

    @Message(routingKey = EVENT_QUEUE1, name = EXCHANGE, exchangeType = DIRECT)
    public record Event1() {
    }

    @Message(routingKey = EVENT_QUEUE2, name = EXCHANGE, exchangeType = DIRECT)
    public record Event2() {
    }

    @Message(routingKey = EVENT_QUEUE3, name = EXCHANGE, exchangeType = DIRECT)
    public record Event3() {
    }
}
