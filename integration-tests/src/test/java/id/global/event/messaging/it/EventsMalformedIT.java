package id.global.event.messaging.it;

import static id.global.common.annotations.amqp.ExchangeType.DIRECT;
import static id.global.common.annotations.amqp.ExchangeType.FANOUT;
import static id.global.common.annotations.amqp.ExchangeType.TOPIC;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.inject.Inject;
import javax.transaction.TransactionManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import id.global.common.annotations.amqp.Message;
import id.global.event.messaging.runtime.EventAppInfoProvider;
import id.global.event.messaging.runtime.HostnameProvider;
import id.global.event.messaging.runtime.channel.ProducerChannelService;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.exception.AmqpSendException;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import id.global.event.messaging.runtime.producer.CorrelationIdProvider;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventsMalformedIT {

    private static final String DIRECT_EXCHANGE = "md-exchange";
    private static final String TOPIC_EXCHANGE = "mt-exchange";
    private static final String FANOUT_EXCHANGE = "mf-exchange";
    private static final String DIRECT_QUEUE = "md-queue";
    private static final String TOPIC_QUEUE = "mt-queue";

    @Inject
    ProducerChannelService producerChannelService;

    @Inject
    AmqpConfiguration configuration;

    @Inject
    EventContext eventContext;

    @Inject
    TransactionManager transactionManager;

    @Inject
    CorrelationIdProvider correlationIdProvider;

    @Inject
    HostnameProvider hostnameProvider;

    @Inject
    EventAppInfoProvider eventAppInfoProvider;

    @Test
    @DisplayName("Exception while serializing events should fail publishing.")
    public void exceptionWhenPublish() throws JsonProcessingException {

        ObjectMapper objectMapper = mock(ObjectMapper.class);

        when(objectMapper.writeValueAsBytes(Mockito.any()))
                .thenThrow(new JsonProcessingException("") {
                });

        AmqpProducer producer = new AmqpProducer(producerChannelService, objectMapper, eventContext, configuration,
                transactionManager, correlationIdProvider, hostnameProvider, eventAppInfoProvider);

        Assertions.assertThrows(AmqpSendException.class, () -> {
            producer.send(new TopicEventTmp("topic", 1L));
        });
        Assertions.assertThrows(AmqpSendException.class, () -> {
            producer.send(new FanoutEventTmp("fanout", 1L));
        });
        Assertions.assertThrows(AmqpSendException.class, () -> {
            producer.send(new DirectEventTmp("direct", 1L));
        });
    }

    @Message(name = DIRECT_EXCHANGE, routingKey = DIRECT_QUEUE, exchangeType = DIRECT)
    private record DirectEventTmp(String name, long age) {
    }

    @Message(name = TOPIC_EXCHANGE, routingKey = TOPIC_QUEUE, exchangeType = TOPIC)
    private record TopicEventTmp(String name, long age) {
    }

    @Message(name = FANOUT_EXCHANGE, exchangeType = FANOUT)
    private record FanoutEventTmp(String name, long age) {
    }
}
