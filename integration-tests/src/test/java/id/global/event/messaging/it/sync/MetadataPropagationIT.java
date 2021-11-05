package id.global.event.messaging.it.sync;

import static id.global.asyncapi.spec.enums.ExchangeType.DIRECT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import com.rabbitmq.client.AMQP;

import id.global.asyncapi.spec.annotations.ConsumedEvent;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.annotations.ProducedEvent;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.exception.AmqpSendException;
import id.global.event.messaging.runtime.exception.AmqpTransactionException;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import id.global.event.messaging.runtime.producer.CorrelationIdProvider;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MetadataPropagationIT {

    private static final String EVENT_QUEUE1 = "queue1";
    private static final String EVENT_QUEUE2 = "queue2";
    private static final String EVENT_QUEUE3 = "queue3";
    private static final String EXCHANGE = "exchange";

    @Inject
    AmqpProducer producer;

    @Inject
    Service1 service1;

    @Inject
    Service2 service2;

    @Inject
    FinalService finalService;

    @InjectMock
    CorrelationIdProvider correlationIdProvider;

    @Test
    @DisplayName("Event published should be accompanied with correlationId to the final service")
    void publishPropagatesCorrelationId() throws Exception {
        final var correlationId = UUID.randomUUID().toString();
        Mockito.when(correlationIdProvider.getCorrelationId()).thenReturn(correlationId);

        producer.send(new Event1());

        final var basicProperties = finalService.getEventContext().get();
        assertThat(basicProperties.getCorrelationId(), is(correlationId));
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class Service1 {
        private final AmqpProducer producer;

        @Inject
        public Service1(EventContext eventContext, AmqpProducer producer) {
            this.producer = producer;
        }

        @MessageHandler
        public void handle(Event1 event) throws AmqpSendException, AmqpTransactionException {
            final var forwardedEvent = new Event2();
            producer.send(forwardedEvent);
        }
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class Service2 {
        private final AmqpProducer producer;

        @Inject
        public Service2(AmqpProducer producer) {
            this.producer = producer;
        }

        @MessageHandler
        public void handle(Event2 event) throws AmqpSendException, AmqpTransactionException {
            final var forwardedEvent = new Event3();
            producer.send(forwardedEvent);
        }
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class FinalService {
        private final CompletableFuture<AMQP.BasicProperties> basicPropertiesCompletableFuture = new CompletableFuture<>();
        private final EventContext eventContext;

        @Inject
        public FinalService(EventContext eventContext) {
            this.eventContext = eventContext;
        }

        @MessageHandler
        public void handle(Event3 event) {
            final var amqpBasicProperties = this.eventContext.getAmqpBasicProperties();
            basicPropertiesCompletableFuture.complete(amqpBasicProperties);
        }

        public CompletableFuture<AMQP.BasicProperties> getEventContext() {
            return basicPropertiesCompletableFuture;
        }
    }

    @ConsumedEvent(queue = EVENT_QUEUE1, exchange = EXCHANGE, exchangeType = DIRECT)
    @ProducedEvent(queue = EVENT_QUEUE1, exchange = EXCHANGE, exchangeType = DIRECT)
    public record Event1() {
    }

    @ConsumedEvent(queue = EVENT_QUEUE2, exchange = EXCHANGE, exchangeType = DIRECT)
    @ProducedEvent(queue = EVENT_QUEUE2, exchange = EXCHANGE, exchangeType = DIRECT)
    public record Event2() {
    }

    @ConsumedEvent(queue = EVENT_QUEUE3, exchange = EXCHANGE, exchangeType = DIRECT)
    @ProducedEvent(queue = EVENT_QUEUE3, exchange = EXCHANGE, exchangeType = DIRECT)
    public record Event3() {
    }

}
