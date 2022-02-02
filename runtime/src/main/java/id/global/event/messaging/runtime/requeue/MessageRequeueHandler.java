package id.global.event.messaging.runtime.requeue;

import static id.global.event.messaging.runtime.Headers.RequeueHeaders.X_ERROR_CODE;
import static id.global.event.messaging.runtime.Headers.RequeueHeaders.X_MAX_RETRIES;
import static id.global.event.messaging.runtime.Headers.RequeueHeaders.X_NOTIFY_CLIENT;
import static id.global.event.messaging.runtime.Headers.RequeueHeaders.X_ORIGINAL_EXCHANGE;
import static id.global.event.messaging.runtime.Headers.RequeueHeaders.X_ORIGINAL_ROUTING_KEY;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

import id.global.event.messaging.runtime.channel.ChannelService;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;

@ApplicationScoped
public class MessageRequeueHandler {

    private static final String RETRY_QUEUE = "retry";
    private static final String RETRY_EXCHANGE = "retry";

    private final Channel channel;
    private final AmqpConfiguration configuration;

    @Inject
    public MessageRequeueHandler(@Named("producerChannelService") ChannelService channelService,
            AmqpConfiguration configuration) throws IOException {

        this.configuration = configuration;
        String channelId = UUID.randomUUID().toString();
        this.channel = channelService.getOrCreateChannelById(channelId);
    }

    public void enqueueWithBackoff(Delivery message, final String errorCode, final boolean shouldNotifyFrontend)
            throws IOException {
        Delivery newMessage = getMessageWithNewHeaders(message, errorCode, shouldNotifyFrontend);
        channel.basicPublish(RETRY_EXCHANGE, RETRY_QUEUE, newMessage.getProperties(), newMessage.getBody());
    }

    private Delivery getMessageWithNewHeaders(Delivery message, final String errorCode, final boolean shouldNotifyFrontend) {
        AMQP.BasicProperties properties = message.getProperties();
        Map<String, Object> headers = properties.getHeaders();

        Map<String, Object> newHeaders = new HashMap<>(headers);
        newHeaders.put(X_ORIGINAL_EXCHANGE, message.getEnvelope().getExchange());
        newHeaders.put(X_ORIGINAL_ROUTING_KEY, message.getEnvelope().getRoutingKey());
        newHeaders.put(X_MAX_RETRIES, configuration.getRetryMaxCount());
        newHeaders.put(X_ERROR_CODE, errorCode);
        newHeaders.put(X_NOTIFY_CLIENT, shouldNotifyFrontend);

        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties().builder().headers(newHeaders)
                .appId(properties.getAppId())
                .correlationId(properties.getCorrelationId())
                .messageId(properties.getMessageId())
                .clusterId(properties.getClusterId())
                .contentEncoding(properties.getContentEncoding())
                .contentType(properties.getContentType())
                .deliveryMode(properties.getDeliveryMode())
                .expiration(properties.getExpiration())
                .priority(properties.getPriority())
                .replyTo(properties.getReplyTo())
                .timestamp(properties.getTimestamp())
                .type(properties.getType())
                .build();

        return new Delivery(message.getEnvelope(), basicProperties, message.getBody());
    }
}
