package id.global.iris.test.tooling.producer;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;

import id.global.iris.common.annotations.Message;
import id.global.iris.common.constants.DeliveryMode;
import id.global.iris.common.constants.Exchanges;
import id.global.iris.messaging.runtime.channel.ChannelService;

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
        final var basicProperties = new AMQP.BasicProperties.Builder()
                .headers(new HashMap<>())
                .deliveryMode(DeliveryMode.PERSISTENT.getValue())
                .build();
        final var eventType = message.getClass().getAnnotation(Message.class).name();
        final var payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(message);
        log.info("Publishing frontend event.\nevent: {}\npayload:\n{}", eventType, payload);
        final var body = objectMapper.writeValueAsBytes(message);
        channel.basicPublish(Exchanges.FRONTEND.getValue(), eventType, basicProperties, body);
    }
}
