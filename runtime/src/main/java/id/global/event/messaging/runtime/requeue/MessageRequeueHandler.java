package id.global.event.messaging.runtime.requeue;

import static id.global.common.headers.amqp.MessagingHeaders.QueueDeclaration.X_DEAD_LETTER_EXCHANGE;
import static id.global.common.headers.amqp.MessagingHeaders.QueueDeclaration.X_DEAD_LETTER_ROUTING_KEY;
import static id.global.common.headers.amqp.MessagingHeaders.RequeueMessage.X_ERROR_CODE;
import static id.global.common.headers.amqp.MessagingHeaders.RequeueMessage.X_MAX_RETRIES;
import static id.global.common.headers.amqp.MessagingHeaders.RequeueMessage.X_NOTIFY_CLIENT;
import static id.global.common.headers.amqp.MessagingHeaders.RequeueMessage.X_ORIGINAL_EXCHANGE;
import static id.global.common.headers.amqp.MessagingHeaders.RequeueMessage.X_ORIGINAL_ROUTING_KEY;

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

import id.global.common.iris.Exchanges;
import id.global.common.iris.Queues;
import id.global.event.messaging.runtime.QueueNameProvider;
import id.global.event.messaging.runtime.channel.ChannelService;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.context.AmqpContext;

@ApplicationScoped
public class MessageRequeueHandler {

    private final Channel channel;
    private final AmqpConfiguration configuration;
    private final QueueNameProvider queueNameProvider;

    @Inject
    public MessageRequeueHandler(@Named("producerChannelService") ChannelService channelService,
            AmqpConfiguration configuration, QueueNameProvider queueNameProvider) throws IOException {

        this.configuration = configuration;
        this.queueNameProvider = queueNameProvider;
        String channelId = UUID.randomUUID().toString();
        this.channel = channelService.getOrCreateChannelById(channelId);
    }

    public void enqueueWithBackoff(final AmqpContext amqpContext, Delivery message,
            final String errorCode, final boolean shouldNotifyFrontend)
            throws IOException {
        Delivery newMessage = getMessageWithNewHeaders(amqpContext, message, errorCode, shouldNotifyFrontend);
        channel.basicPublish(Exchanges.RETRY.getValue(), Queues.RETRY.getValue(), newMessage.getProperties(),
                newMessage.getBody());
    }

    private Delivery getMessageWithNewHeaders(final AmqpContext amqpContext,
            Delivery message, final String errorCode, final boolean shouldNotifyFrontend) {
        AMQP.BasicProperties properties = message.getProperties();
        Map<String, Object> headers = properties.getHeaders();

        Map<String, Object> newHeaders = new HashMap<>(headers);
        newHeaders.put(X_ORIGINAL_EXCHANGE, message.getEnvelope().getExchange());
        newHeaders.put(X_ORIGINAL_ROUTING_KEY, message.getEnvelope().getRoutingKey());
        newHeaders.put(X_MAX_RETRIES, configuration.getRetryMaxCount());
        newHeaders.put(X_ERROR_CODE, errorCode);
        newHeaders.put(X_NOTIFY_CLIENT, shouldNotifyFrontend);

        final var deadLetterExchangeName = amqpContext.getDeadLetterExchangeName();
        if (deadLetterExchangeName.isPresent()) {
            final var queueName = queueNameProvider.getQueueName(amqpContext);
            final var deadLetterRoutingKey = amqpContext.getDeadLetterRoutingKey(queueName);
            newHeaders.put(X_DEAD_LETTER_EXCHANGE, deadLetterExchangeName.get());
            newHeaders.put(X_DEAD_LETTER_ROUTING_KEY, deadLetterRoutingKey);
        }

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
