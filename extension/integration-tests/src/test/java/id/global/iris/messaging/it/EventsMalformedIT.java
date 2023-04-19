package id.global.iris.messaging.it;

import static id.global.iris.common.annotations.ExchangeType.DIRECT;
import static id.global.iris.common.annotations.ExchangeType.FANOUT;
import static id.global.iris.common.annotations.ExchangeType.TOPIC;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.TransactionManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import id.global.iris.common.annotations.Message;
import id.global.iris.messaging.runtime.BasicPropertiesProvider;
import id.global.iris.messaging.runtime.channel.ChannelService;
import id.global.iris.messaging.runtime.configuration.IrisRabbitMQConfig;
import id.global.iris.messaging.runtime.exception.IrisSendException;
import id.global.iris.messaging.runtime.producer.EventProducer;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventsMalformedIT extends IsolatedEventContextTest {

    private static final String DIRECT_EXCHANGE = "md-exchange";
    private static final String TOPIC_EXCHANGE = "mt-exchange";
    private static final String FANOUT_EXCHANGE = "mf-exchange";
    private static final String DIRECT_QUEUE = "md-queue";
    private static final String TOPIC_QUEUE = "mt-queue";

    @Inject
    @Named("producerChannelService")
    ChannelService producerChannelService;

    @Inject
    IrisRabbitMQConfig resilienceConfig;

    @Inject
    TransactionManager transactionManager;

    @Inject
    BasicPropertiesProvider basicPropertiesProvider;

    @Test
    @DisplayName("Exception while serializing events should fail publishing.")
    public void exceptionWhenPublish() throws JsonProcessingException {

        ObjectMapper objectMapper = mock(ObjectMapper.class);

        when(objectMapper.writeValueAsBytes(Mockito.any()))
                .thenThrow(new JsonProcessingException("") {
                });

        EventProducer producer = new EventProducer(producerChannelService, objectMapper, eventContext, resilienceConfig,
                transactionManager, basicPropertiesProvider);

        Assertions.assertThrows(IrisSendException.class, () -> {
            producer.send(new TopicEventTmp("topic", 1L));
        });
        Assertions.assertThrows(IrisSendException.class, () -> {
            producer.send(new FanoutEventTmp("fanout", 1L));
        });
        Assertions.assertThrows(IrisSendException.class, () -> {
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
