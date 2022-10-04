package id.global.iris.messaging.runtime.requeue;

import static id.global.iris.common.constants.MessagingHeaders.Message.SERVER_TIMESTAMP;
import static id.global.iris.common.constants.MessagingHeaders.QueueDeclaration.X_DEAD_LETTER_EXCHANGE;
import static id.global.iris.common.constants.MessagingHeaders.QueueDeclaration.X_DEAD_LETTER_ROUTING_KEY;
import static id.global.iris.common.constants.MessagingHeaders.RequeueMessage.X_ERROR_CODE;
import static id.global.iris.common.constants.MessagingHeaders.RequeueMessage.X_ERROR_MESSAGE;
import static id.global.iris.common.constants.MessagingHeaders.RequeueMessage.X_ERROR_TYPE;
import static id.global.iris.common.constants.MessagingHeaders.RequeueMessage.X_MAX_RETRIES;
import static id.global.iris.common.constants.MessagingHeaders.RequeueMessage.X_NOTIFY_CLIENT;
import static id.global.iris.common.constants.MessagingHeaders.RequeueMessage.X_ORIGINAL_EXCHANGE;
import static id.global.iris.common.constants.MessagingHeaders.RequeueMessage.X_ORIGINAL_ROUTING_KEY;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

import id.global.iris.common.constants.Exchanges;
import id.global.iris.common.constants.Queues;
import id.global.iris.common.exception.MessagingException;
import id.global.iris.messaging.runtime.QueueNameProvider;
import id.global.iris.messaging.runtime.TimestampProvider;
import id.global.iris.messaging.runtime.channel.ChannelService;
import id.global.iris.messaging.runtime.configuration.IrisRabbitMQConfig;
import id.global.iris.messaging.runtime.context.IrisContext;

@ApplicationScoped
public class MessageRequeueHandler {

    private final Channel channel;
    private final IrisRabbitMQConfig config;
    private final QueueNameProvider queueNameProvider;
    private final TimestampProvider timestampProvider;

    @Inject
    public MessageRequeueHandler(@Named("producerChannelService") ChannelService channelService,
            IrisRabbitMQConfig config,
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
        newHeaders.put(X_MAX_RETRIES, config.getRetryMaxCount());
        newHeaders.put(X_ERROR_CODE, messagingException.getClientCode());
        newHeaders.put(X_ERROR_TYPE, messagingException.getErrorType());
        newHeaders.put(X_ERROR_MESSAGE, messagingException.getMessage());
        newHeaders.put(X_NOTIFY_CLIENT, shouldNotifyFrontend);
        newHeaders.put(SERVER_TIMESTAMP, timestampProvider.getCurrentTimestamp());

        final var deadLetterExchangeName = irisContext.getDeadLetterExchangeName();
        if (deadLetterExchangeName.isPresent()) {
            final var queueName = queueNameProvider.getQueueName(irisContext);
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
