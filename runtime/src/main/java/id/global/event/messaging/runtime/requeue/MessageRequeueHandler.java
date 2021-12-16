package id.global.event.messaging.runtime.requeue;

import static id.global.event.messaging.runtime.Headers.QueueDeclarationHeaders.X_DEAD_LETTER_EXCHANGE;
import static id.global.event.messaging.runtime.Headers.QueueDeclarationHeaders.X_DEAD_LETTER_ROUTING_KEY;
import static id.global.event.messaging.runtime.Headers.QueueDeclarationHeaders.X_MESSAGE_TTL;
import static id.global.event.messaging.runtime.Headers.RequeueHeaders.X_ORIGINAL_EXCHANGE;
import static id.global.event.messaging.runtime.Headers.RequeueHeaders.X_ORIGINAL_ROUTING_KEY;
import static id.global.event.messaging.runtime.Headers.RequeueHeaders.X_RETRY_COUNT;
import static id.global.event.messaging.runtime.requeue.MessageRequeueConsumer.RETRY_EXCHANGE;
import static id.global.event.messaging.runtime.requeue.MessageRequeueConsumer.RETRY_WAIT_ENDED_QUEUE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

import id.global.event.messaging.runtime.channel.ChannelService;

@ApplicationScoped
public class MessageRequeueHandler {
    private final RetryQueues retryQueues;
    private final Channel channel;

    public MessageRequeueHandler(@Named("producerChannelService") ChannelService channelService,
            RetryQueues retryQueues) throws IOException {
        String channelId = UUID.randomUUID().toString();
        this.channel = channelService.getOrCreateChannelById(channelId);
        this.retryQueues = retryQueues;
    }

    public void enqueueWithBackoff(Delivery message, int retryCount) throws IOException {
        RetryQueue retryQueue = retryQueues.getNextQueue(retryCount);
        String retryQueueName = retryQueue.queueName();

        setOriginHeaders(message);
        incrementRetryCount(message, retryCount);

        final Map<String, Object> queueDeclarationArgs = getRequeueDeclarationParams(retryQueue);

        channel.exchangeDeclare(RETRY_EXCHANGE, BuiltinExchangeType.DIRECT);
        channel.queueDeclare(retryQueueName, false, false, true, queueDeclarationArgs);
        channel.queueBind(retryQueueName, RETRY_EXCHANGE, retryQueueName);

        channel.basicPublish(RETRY_EXCHANGE, retryQueueName, message.getProperties(), message.getBody());
    }

    private Map<String, Object> getRequeueDeclarationParams(RetryQueue retryQueue) {
        final Map<String, Object> queueDeclarationArgs = new HashMap<>();
        queueDeclarationArgs.put(X_MESSAGE_TTL, retryQueue.ttl());
        queueDeclarationArgs.put(X_DEAD_LETTER_ROUTING_KEY, RETRY_WAIT_ENDED_QUEUE);
        queueDeclarationArgs.put(X_DEAD_LETTER_EXCHANGE, RETRY_EXCHANGE);
        return queueDeclarationArgs;
    }

    private void setOriginHeaders(Delivery message) {
        message.getProperties().getHeaders().put(X_ORIGINAL_EXCHANGE,
                message.getEnvelope().getExchange());
        message.getProperties().getHeaders().put(X_ORIGINAL_ROUTING_KEY,
                message.getEnvelope().getRoutingKey());
    }

    private void incrementRetryCount(Delivery message, int retryCount) {
        retryCount = retryCount + 1;
        message.getProperties().getHeaders().put(X_RETRY_COUNT, retryCount);
    }
}
