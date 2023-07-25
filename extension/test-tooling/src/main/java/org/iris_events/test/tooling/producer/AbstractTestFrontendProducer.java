package org.iris_events.test.tooling.producer;

import static org.iris_events.common.MessagingHeaders.Message.EVENT_TYPE;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.iris_events.annotations.Message;
import org.iris_events.common.DeliveryMode;
import org.iris_events.common.Exchanges;
import org.iris_events.runtime.channel.ChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;

public abstract class AbstractTestFrontendProducer {
    private static final Logger log = LoggerFactory.getLogger(AbstractTestFrontendProducer.class);

    private final ChannelService channelService;
    private final ObjectMapper objectMapper;

    public AbstractTestFrontendProducer(final ChannelService channelService, final ObjectMapper objectMapper) {
        this.channelService = channelService;
        this.objectMapper = objectMapper;
    }

    public void send(final Object message) throws IOException {
        final var channel = channelService.getOrCreateChannelById(UUID.randomUUID().toString());
        final var eventType = message.getClass().getAnnotation(Message.class).name();
        final var headers = Map.of(EVENT_TYPE, (Object) eventType);
        final var basicProperties = new AMQP.BasicProperties.Builder()
                .headers(headers)
                .deliveryMode(DeliveryMode.PERSISTENT.getValue())
                .build();
        final var payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(message);
        log.info("Publishing frontend event.\nevent: {}\npayload:\n{}", eventType, payload);
        final var body = objectMapper.writeValueAsBytes(message);
        channel.basicPublish(Exchanges.FRONTEND.getValue(), eventType, basicProperties, body);
    }
}
