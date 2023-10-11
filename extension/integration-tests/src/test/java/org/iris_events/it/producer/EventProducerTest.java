package org.iris_events.it.producer;

import static org.iris_events.common.MessagingHeaders.Message.CURRENT_SERVICE_ID;
import static org.iris_events.common.MessagingHeaders.Message.EVENT_TYPE;
import static org.iris_events.common.MessagingHeaders.Message.INSTANCE_ID;
import static org.iris_events.common.MessagingHeaders.Message.ORIGIN_SERVICE_ID;
import static org.iris_events.common.MessagingHeaders.Message.SERVER_TIMESTAMP;
import static org.iris_events.common.MessagingHeaders.Message.USER_ID;
import static org.iris_events.producer.EventProducer.SERVICE_ID_UNAVAILABLE_FALLBACK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.iris_events.annotations.Message;
import org.iris_events.annotations.Scope;
import org.iris_events.common.DeliveryMode;
import org.iris_events.common.Exchanges;
import org.iris_events.common.message.ResourceMessage;
import org.iris_events.context.EventContext;
import org.iris_events.producer.CorrelationIdProvider;
import org.iris_events.producer.EventProducer;
import org.iris_events.producer.SendOptions;
import org.iris_events.runtime.EventAppInfoProvider;
import org.iris_events.runtime.InstanceInfoProvider;
import org.iris_events.runtime.TimestampProvider;
import org.iris_events.runtime.channel.ChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventProducerTest {

    private static final long CURRENT_TIMESTAMP = Instant.now().toEpochMilli();
    private static final String AMQP_PRODUCER_TEST_SESSION_EVENT = "amqp-producer-test-session-event";
    private static final String AMQP_PRODUCER_TEST_USER_EVENT = "amqp_producer_test_user_event";
    private static final String AMQP_PRODUCER_TEST_INTERNAL_EVENT = "amqp-producer-test-internal-event";
    private static final String AMQP_PRODUCER_TEST_CORRELATION_ID_EVENT = "amqp-producer-test-correlation-id-event";
    private static final String TEST_CORRELATION_ID = "correlationIdOverride";
    private static final String INSTANCE_NAME = "AmqpProducerTestInstance";

    @Inject
    EventProducer producer;

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
    }

    @ParameterizedTest
    @MethodSource
    void send(Object event, String eventName, String expectedExchange, String expectedRoutingKey, DeliveryMode deliveryMode)
            throws Exception {
        mockBasicProperties(eventName);

        producer.send(event);

        final byte[] bytes = objectMapper.writeValueAsBytes(event);
        final var basicProperties = getBasicPropertiesBuilder(eventName, null, null, deliveryMode).build();
        Mockito.verify(channel)
                .basicPublish(expectedExchange,
                        expectedRoutingKey,
                        true,
                        basicProperties,
                        bytes);
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> send() {
        return Stream.of(
                Arguments.of(getSessionEvent(), AMQP_PRODUCER_TEST_SESSION_EVENT, Exchanges.SESSION.getValue(),
                        AMQP_PRODUCER_TEST_SESSION_EVENT + "." + Exchanges.SESSION.getValue(), DeliveryMode.NON_PERSISTENT),
                Arguments.of(getUserEvent(), AMQP_PRODUCER_TEST_USER_EVENT, Exchanges.USER.getValue(),
                        AMQP_PRODUCER_TEST_USER_EVENT + "." + Exchanges.USER.getValue(), DeliveryMode.NON_PERSISTENT),
                Arguments.of(getInternalEvent(), AMQP_PRODUCER_TEST_INTERNAL_EVENT, AMQP_PRODUCER_TEST_INTERNAL_EVENT, "",
                        DeliveryMode.PERSISTENT));
    }

    @Test
    void sendOverrideCorrelationId() throws IOException {
        mockBasicProperties(AMQP_PRODUCER_TEST_CORRELATION_ID_EVENT);
        var event = getInternalCorrelationIdEvent();

        SendOptions sendOptions = new SendOptions(null, TEST_CORRELATION_ID);
        producer.send(event, sendOptions);

        final byte[] bytes = objectMapper.writeValueAsBytes(event);
        final var basicProperties = getBasicPropertiesBuilder(AMQP_PRODUCER_TEST_CORRELATION_ID_EVENT, null,
                TEST_CORRELATION_ID, DeliveryMode.PERSISTENT).build();
        Mockito.verify(channel)
                .basicPublish(AMQP_PRODUCER_TEST_CORRELATION_ID_EVENT,
                        "",
                        true,
                        basicProperties,
                        bytes);
    }

    @Test
    void sendToSubscription() throws Exception {
        mockBasicProperties(AMQP_PRODUCER_TEST_SESSION_EVENT);

        final var resourceType = "amqpProducerTestEventResource";
        final var resourceId = UUID.randomUUID().toString();
        final var event = new SessionEvent(resourceId);
        final var basicProperties = getBasicPropertiesBuilder(AMQP_PRODUCER_TEST_SESSION_EVENT, null, null,
                DeliveryMode.NON_PERSISTENT).build();

        producer.sendToSubscription(event, resourceType, resourceId);

        final var resourceUpdate = new ResourceMessage(resourceType, resourceId, event);
        final byte[] bytes = objectMapper.writeValueAsBytes(resourceUpdate);
        Mockito.verify(channel)
                .basicPublish(Exchanges.SUBSCRIPTION.getValue(),
                        AMQP_PRODUCER_TEST_SESSION_EVENT + ".resource",
                        true,
                        basicProperties,
                        bytes);
    }

    @Test
    void sendToUser() throws Exception {
        mockBasicProperties(AMQP_PRODUCER_TEST_SESSION_EVENT);
        final var event = getSessionEvent();
        final var userId = UUID.randomUUID().toString();

        producer.send(event, userId);

        final byte[] bytes = objectMapper.writeValueAsBytes(event);
        final var basicProperties = getBasicPropertiesBuilder(AMQP_PRODUCER_TEST_SESSION_EVENT, userId, null,
                DeliveryMode.NON_PERSISTENT).build();
        Mockito.verify(channel)
                .basicPublish(Exchanges.USER.getValue(),
                        AMQP_PRODUCER_TEST_SESSION_EVENT + "." + Exchanges.USER.getValue(),
                        true,
                        basicProperties,
                        bytes);
    }

    private void mockBasicProperties(final String eventName) {
        final var basicPropertiesMock = mock(AMQP.BasicProperties.class);
        final var basicPropertiesBuilder = getBasicPropertiesBuilder(eventName, null, null, DeliveryMode.NON_PERSISTENT);
        when(basicPropertiesMock.builder()).thenReturn(basicPropertiesBuilder);
        when(eventContext.getAmqpBasicProperties()).thenReturn(basicPropertiesMock);
    }

    private AMQP.BasicProperties.Builder getBasicPropertiesBuilder(final String eventName, final String userId,
            final String correlationId,
            final DeliveryMode deliveryMode) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(CURRENT_SERVICE_ID, SERVICE_ID_UNAVAILABLE_FALLBACK);
        headers.put(INSTANCE_ID, INSTANCE_NAME);
        headers.put(EVENT_TYPE, eventName);
        headers.put(SERVER_TIMESTAMP, CURRENT_TIMESTAMP);

        if (userId != null) {
            headers.put(ORIGIN_SERVICE_ID, SERVICE_ID_UNAVAILABLE_FALLBACK);
            headers.put(USER_ID, userId);
        }

        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();
        if (correlationId != null) {
            builder.correlationId(correlationId);
        }
        return builder
                .deliveryMode(deliveryMode.getValue())
                .headers(headers);
    }

    private static SessionEvent getSessionEvent() {
        final var resourceId = UUID.randomUUID().toString();
        return new SessionEvent(resourceId);
    }

    private static UserEvent getUserEvent() {
        final var resourceId = UUID.randomUUID().toString();
        return new UserEvent(resourceId);
    }

    private static InternalEvent getInternalEvent() {
        final var resourceId = UUID.randomUUID().toString();
        return new InternalEvent(resourceId);
    }

    private static InternalCorrelationIdEvent getInternalCorrelationIdEvent() {
        final var resourceId = UUID.randomUUID().toString();
        return new InternalCorrelationIdEvent(resourceId);
    }

    @Message(name = AMQP_PRODUCER_TEST_SESSION_EVENT, scope = Scope.SESSION)
    static final class SessionEvent {
        private final String id;

        SessionEvent(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    @Message(name = AMQP_PRODUCER_TEST_USER_EVENT, scope = Scope.USER)
    static final class UserEvent {
        private final String id;

        UserEvent(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    @Message(name = AMQP_PRODUCER_TEST_INTERNAL_EVENT, persistent = true)
    static final class InternalEvent {
        private final String id;

        InternalEvent(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    @Message(name = AMQP_PRODUCER_TEST_CORRELATION_ID_EVENT, persistent = true)
    static final class InternalCorrelationIdEvent {
        private final String id;

        InternalCorrelationIdEvent(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }
}
