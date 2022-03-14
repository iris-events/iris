package id.global.iris.messaging.it.producer;

import static id.global.common.headers.amqp.MessagingHeaders.Message.CURRENT_SERVICE_ID;
import static id.global.common.headers.amqp.MessagingHeaders.Message.EVENT_TYPE;
import static id.global.common.headers.amqp.MessagingHeaders.Message.INSTANCE_ID;
import static id.global.common.headers.amqp.MessagingHeaders.Message.SERVER_TIMESTAMP;
import static id.global.iris.messaging.runtime.producer.AmqpProducer.SERVICE_ID_UNAVAILABLE_FALLBACK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.Scope;
import id.global.common.iris.Exchanges;
import id.global.iris.messaging.runtime.EventAppInfoProvider;
import id.global.iris.messaging.runtime.InstanceInfoProvider;
import id.global.iris.messaging.runtime.TimestampProvider;
import id.global.iris.messaging.runtime.api.message.ResourceUpdate;
import id.global.iris.messaging.runtime.channel.ChannelService;
import id.global.iris.messaging.runtime.context.EventContext;
import id.global.iris.messaging.runtime.producer.AmqpProducer;
import id.global.iris.messaging.runtime.producer.CorrelationIdProvider;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AmqpProducerTest {

    public static final long CURRENT_TIMESTAMP = Instant.now().toEpochMilli();
    public static final String AMQP_PRODUCER_TEST_EVENT = "AMQP-PRODUCER-TEST-EVENT";
    public static final String INSTANCE_NAME = "AmqpProducerTestInstance";
    private static final AMQP.BasicProperties.Builder DEFAULT_BASIC_PROPERTIES_BUILDER = getBasicPropertiesBuilder();
    private static final AMQP.BasicProperties DEFAULT_BASIC_PROPERTIES = mockBasicProperties();

    private static AMQP.BasicProperties mockBasicProperties() {
        final var mock = mock(AMQP.BasicProperties.class);
        Mockito.when(mock.builder()).thenReturn(DEFAULT_BASIC_PROPERTIES_BUILDER);
        return mock;
    }

    private static AMQP.BasicProperties.Builder getBasicPropertiesBuilder() {
        return new AMQP.BasicProperties().builder()
                .headers(Map.of(CURRENT_SERVICE_ID, SERVICE_ID_UNAVAILABLE_FALLBACK,
                        INSTANCE_ID, INSTANCE_NAME,
                        EVENT_TYPE, "subscription",
                        SERVER_TIMESTAMP, CURRENT_TIMESTAMP));
    }

    @Inject
    AmqpProducer producer;
    @Inject
    ObjectMapper objectMapper;

    @InjectMock
    @Named("producerChannelService")
    ChannelService channelService;
    @InjectMock
    EventContext eventContext;
    @InjectMock
    CorrelationIdProvider correlationIdProvider;
    @InjectMock
    InstanceInfoProvider instanceInfoProvider;
    @InjectMock
    EventAppInfoProvider eventAppInfoProvider;
    @InjectMock
    TimestampProvider timestampProvider;

    private Channel channel;

    @BeforeEach
    void init() throws IOException {
        channel = mock(Channel.class);
        when(channelService.getOrCreateChannelById(any())).thenReturn(channel);
        when(timestampProvider.getCurrentTimestamp()).thenReturn(CURRENT_TIMESTAMP);
        when(instanceInfoProvider.getInstanceName()).thenReturn(INSTANCE_NAME);
        when(eventContext.getAmqpBasicProperties()).thenReturn(DEFAULT_BASIC_PROPERTIES);
    }

    @Test
    void sendToSubscription() throws Exception {
        final var resourceType = "amqpProducerTestEventResource";
        final var resourceId = UUID.randomUUID().toString();
        final var event = new Event(resourceId);
        final var basicProperties = DEFAULT_BASIC_PROPERTIES_BUILDER.build();

        producer.sendToSubscription(event, resourceType, resourceId);
        final var resourceUpdate = new ResourceUpdate(resourceType, resourceId, Scope.SESSION, event);
        final byte[] bytes = objectMapper.writeValueAsBytes(resourceUpdate);

        Mockito.verify(channel)
                .basicPublish(Exchanges.SUBSCRIPTION.getValue(), AMQP_PRODUCER_TEST_EVENT, true, basicProperties,
                        bytes);
    }

    @Message(name = AMQP_PRODUCER_TEST_EVENT, scope = Scope.SESSION)
    record Event(String id) {
    }
}
