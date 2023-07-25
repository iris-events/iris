package org.iris_events.test.tooling.consumer;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

import org.iris_events.common.MessagingHeaders;
import org.iris_events.common.message.ErrorMessage;
import org.iris_events.runtime.channel.ChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public abstract class AbstractTestConsumer {
    private static final Logger log = LoggerFactory.getLogger(AbstractTestConsumer.class);

    private final ChannelService channelService;
    private final ObjectMapper objectMapper;

    public AbstractTestConsumer(final ChannelService channelService, final ObjectMapper objectMapper) {
        this.channelService = channelService;
        this.objectMapper = objectMapper;
    }

    public void startSessionConsumer(final Class<?> messageClass, String eventName, Consumer<Object> action)
            throws IOException {
        final var channel = channelService.getOrCreateChannelById(UUID.randomUUID().toString());
        final var queueName = UUID.randomUUID().toString();

        channel.queueDeclare(queueName, false, true, true, Collections.emptyMap());
        channel.queueBind(queueName, "session", eventName + ".*");

        channel.basicConsume(queueName, false, "testSessionConsumer", new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(final String consumerTag, final Envelope envelope,
                    final AMQP.BasicProperties properties,
                    final byte[] body) throws IOException {
                final var jsonNode = getJsonNode(body);
                final var eventType = getEventType(properties);
                final var payload = getPayload(jsonNode);
                log.info("TestConsumer received SESSION event.\nevent: {}\nexchange: {}\nrouting key: {}\npayload:\n{}",
                        eventType, envelope.getExchange(), envelope.getRoutingKey(), payload);
                tryExtractValue(body, messageClass, action);
            }
        });

    }

    public void startErrorConsumer(Consumer<Object> action) throws IOException {
        final var channel = channelService.getOrCreateChannelById(UUID.randomUUID().toString());
        final var queueName = UUID.randomUUID().toString();
        channel.queueDeclare(queueName, false, true, true, Collections.emptyMap());
        channel.queueBind(queueName, "error", "*.*");
        channel.basicConsume(queueName, false, "testSessionConsumer",
                new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag,
                            Envelope envelope,
                            AMQP.BasicProperties properties,
                            byte[] body)
                            throws IOException {
                        long deliveryTag = envelope.getDeliveryTag();
                        final var eventType = getEventType(properties);
                        channel.basicAck(deliveryTag, false);
                        final var jsonNode = getJsonNode(body);
                        final var payload = getPayload(jsonNode);
                        log.info("TestConsumer received ERROR event.\nevent: {}\nexchange: {}\nrouting key: {}\npayload:\n{}",
                                eventType, envelope.getExchange(), envelope.getRoutingKey(), payload);
                        tryExtractValue(body, ErrorMessage.class, action);
                    }
                });
    }

    private String getEventType(final AMQP.BasicProperties properties) {
        return properties.getHeaders().get(MessagingHeaders.Message.EVENT_TYPE).toString();
    }

    private String getPayload(final JsonNode jsonNode) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
    }

    private JsonNode getJsonNode(final byte[] body) throws IOException {
        return objectMapper.readValue(body, JsonNode.class);
    }

    private void tryExtractValue(final byte[] body, final Class valueType, Consumer<Object> action) {
        try {
            final var value = objectMapper.readValue(body, valueType);
            action.accept(value);
        } catch (IOException ignored) {
            log.error("Could not extract value, exception ignored.", ignored);
        }
    }
}
