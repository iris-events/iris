package org.iris_events.runtime.requeue;

import static org.iris_events.common.MessagingHeaders.Message.SERVER_TIMESTAMP;
import static org.iris_events.common.MessagingHeaders.QueueDeclaration.X_DEAD_LETTER_EXCHANGE;
import static org.iris_events.common.MessagingHeaders.QueueDeclaration.X_DEAD_LETTER_ROUTING_KEY;
import static org.iris_events.common.MessagingHeaders.RequeueMessage.X_ERROR_CODE;
import static org.iris_events.common.MessagingHeaders.RequeueMessage.X_ERROR_MESSAGE;
import static org.iris_events.common.MessagingHeaders.RequeueMessage.X_ERROR_TYPE;
import static org.iris_events.common.MessagingHeaders.RequeueMessage.X_MAX_RETRIES;
import static org.iris_events.common.MessagingHeaders.RequeueMessage.X_NOTIFY_CLIENT;
import static org.iris_events.common.MessagingHeaders.RequeueMessage.X_ORIGINAL_EXCHANGE;
import static org.iris_events.common.MessagingHeaders.RequeueMessage.X_ORIGINAL_QUEUE;
import static org.iris_events.common.MessagingHeaders.RequeueMessage.X_ORIGINAL_ROUTING_KEY;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.iris_events.common.Exchanges;
import org.iris_events.common.Queues;
import org.iris_events.context.IrisContext;
import org.iris_events.exception.MessagingException;
import org.iris_events.runtime.QueueNameProvider;
import org.iris_events.runtime.TimestampProvider;
import org.iris_events.runtime.channel.ChannelService;
import org.iris_events.runtime.configuration.IrisConfig;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

@ApplicationScoped
public class MessageRequeueHandler {

    private final Channel channel;
    private final IrisConfig config;
    private final QueueNameProvider queueNameProvider;
    private final TimestampProvider timestampProvider;

    @Inject
    public MessageRequeueHandler(@Named("producerChannelService") ChannelService channelService,
            IrisConfig config,
            QueueNameProvider queueNameProvider, TimestampProvider timestampProvider) throws IOException {
        this.config = config;
        this.queueNameProvider = queueNameProvider;
        this.timestampProvider = timestampProvider;
        final var channelId = UUID.randomUUID().toString();
        this.channel = channelService.getOrCreateChannelById(channelId);
    }

    public void enqueueWithBackoff(final IrisContext irisContext, Delivery message,
            final MessagingException messagingException, final boolean shouldNotifyFrontend)
            throws IOException {
        final var newMessage = getMessageWithNewHeaders(irisContext, message, messagingException, shouldNotifyFrontend);
        channel.basicPublish(Exchanges.RETRY.getValue(), Queues.RETRY.getValue(), newMessage.getProperties(),
                newMessage.getBody());
    }

    private Delivery getMessageWithNewHeaders(final IrisContext irisContext,
            Delivery message, final MessagingException messagingException, final boolean shouldNotifyFrontend) {
        final var properties = message.getProperties();
        final var headers = properties.getHeaders();
        final var newHeaders = new HashMap<String, Object>(headers);

        newHeaders.put(X_ORIGINAL_EXCHANGE, message.getEnvelope().getExchange());
        newHeaders.put(X_ORIGINAL_ROUTING_KEY, message.getEnvelope().getRoutingKey());
        newHeaders.put(X_MAX_RETRIES, config.retryMaxCount());
        newHeaders.put(X_ERROR_CODE, messagingException.getClientCode());
        newHeaders.put(X_ERROR_TYPE, messagingException.getErrorType().name());
        newHeaders.put(X_ERROR_MESSAGE, messagingException.getMessage());
        newHeaders.put(X_NOTIFY_CLIENT, shouldNotifyFrontend);
        newHeaders.put(SERVER_TIMESTAMP, timestampProvider.getCurrentTimestamp());
        final var queueName = queueNameProvider.getQueueName(irisContext);
        newHeaders.put(X_ORIGINAL_QUEUE, queueName);

        final var deadLetterExchangeName = irisContext.getDeadLetterExchangeName();
        if (deadLetterExchangeName.isPresent()) {
            final var deadLetterRoutingKey = irisContext.getDeadLetterRoutingKey(queueName);
            newHeaders.put(X_DEAD_LETTER_EXCHANGE, deadLetterExchangeName.get());
            newHeaders.put(X_DEAD_LETTER_ROUTING_KEY, deadLetterRoutingKey);
        }

        final var basicProperties = new AMQP.BasicProperties().builder().headers(newHeaders)
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
