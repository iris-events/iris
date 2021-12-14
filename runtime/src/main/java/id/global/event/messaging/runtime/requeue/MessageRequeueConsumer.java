package id.global.event.messaging.runtime.requeue;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;

import id.global.event.messaging.runtime.Headers;
import id.global.event.messaging.runtime.channel.ChannelService;
import id.global.event.messaging.runtime.exception.AmqpConnectionException;

@ApplicationScoped
public class MessageRequeueConsumer {
    private static final Logger log = LoggerFactory.getLogger(MessageRequeueConsumer.class);

    public static final String RETRY_EXCHANGE = "retry";
    public static final String RETRY_WAIT_ENDED_QUEUE = "retry-wait-ended";

    private final ChannelService channelService;
    private final String channelId;

    @Inject
    public MessageRequeueConsumer(@Named("consumerChannelService") final ChannelService channelService) {
        this.channelService = channelService;
        this.channelId = UUID.randomUUID().toString();
    }

    public void initRetryConsumer() {
        try {
            Channel channel = channelService.getOrCreateChannelById(channelId);

            channel.exchangeDeclare(RETRY_EXCHANGE, BuiltinExchangeType.DIRECT);
            channel.queueDeclare(RETRY_WAIT_ENDED_QUEUE, true, false, false, null);
            channel.queueBind(RETRY_WAIT_ENDED_QUEUE, RETRY_EXCHANGE, RETRY_WAIT_ENDED_QUEUE);

            channel.basicConsume(RETRY_WAIT_ENDED_QUEUE, true, ((consumerTag, message) -> {

                // this relays messages from RETRY queues to original queues
                Map<String, Object> headers = message.getProperties().getHeaders();
                String originalExchange = Objects.toString(headers.get(Headers.RequeueHeaders.X_ORIGINAL_EXCHANGE));
                String originalRoutingKey = Objects.toString(headers.get(Headers.RequeueHeaders.X_ORIGINAL_ROUTING_KEY));

                channel.basicPublish(originalExchange, originalRoutingKey, message.getProperties(), message.getBody());
            }), consumerTag -> {
                log.warn(String.format("Basic consume on %s.%s cancelled. Message for will not be retried", RETRY_EXCHANGE,
                        RETRY_WAIT_ENDED_QUEUE));
            }, (consumerTag, sig) -> {
                log.warn(String.format("Consumer for %s.%s shut down.", RETRY_EXCHANGE, RETRY_WAIT_ENDED_QUEUE));
            });
        } catch (IOException e) {
            String msg = "Could not initialize retry consumer";
            log.error(msg, e);
            throw new AmqpConnectionException(msg, e);
        }

    }
}
