package id.global.iris.messaging.runtime.consumer;

import static id.global.common.headers.amqp.MessagingHeaders.QueueDeclaration.X_DEAD_LETTER_EXCHANGE;
import static id.global.common.headers.amqp.MessagingHeaders.QueueDeclaration.X_DEAD_LETTER_ROUTING_KEY;
import static id.global.common.headers.amqp.MessagingHeaders.QueueDeclaration.X_MESSAGE_TTL;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.ShutdownSignalException;

import id.global.common.annotations.amqp.ExchangeType;
import id.global.iris.messaging.runtime.QueueNameProvider;
import id.global.iris.messaging.runtime.channel.ChannelService;
import id.global.iris.messaging.runtime.context.AmqpContext;

public class AmqpConsumer {
    private static final Logger log = LoggerFactory.getLogger(AmqpConsumer.class);

    private final AmqpContext context;
    private final ChannelService channelService;
    private final DeliverCallbackProvider deliverCallbackProvider;
    private final QueueNameProvider queueNameProvider;

    private DeliverCallback callback;
    private String channelId;

    public AmqpConsumer(
            final AmqpContext context,
            final ChannelService channelService,
            final DeliverCallbackProvider deliverCallbackProvider,
            final QueueNameProvider queueNameProvider) {

        this.context = context;
        this.channelService = channelService;
        this.deliverCallbackProvider = deliverCallbackProvider;
        this.queueNameProvider = queueNameProvider;
        this.channelId = UUID.randomUUID().toString();
    }

    public void initChannel() throws IOException {
        final var channel = channelService.getOrCreateChannelById(this.channelId);
        this.callback = deliverCallbackProvider.createDeliverCallback(channel);
        final var exchangeType = context.exchangeType();
        validateBindingKeys(context.getBindingKeys(), exchangeType);
        createQueues(channel, exchangeType);
    }

    public DeliverCallback getCallback() {
        return callback;
    }

    protected AmqpContext getContext() {
        return context;
    }

    private void createQueues(Channel channel, final ExchangeType exchangeType) throws IOException {
        final var queueDeclarationArgs = new HashMap<String, Object>();
        final var exchange = context.getName();
        final var consumerOnEveryInstance = context.isConsumerOnEveryInstance();
        final var queueName = queueNameProvider.getQueueName(context);

        // set prefetch count "quality of service"
        final int prefetchCount = context.getPrefetch();
        channel.basicQos(prefetchCount);

        // time to leave of queue
        final long ttl = context.getTtl();
        if (ttl >= 0) {
            queueDeclarationArgs.put(X_MESSAGE_TTL, ttl);
        }

        // setup dead letter queue
        final var optionalPrefixedDeadLetterQueue = context.getDeadLetterQueueName();
        if (optionalPrefixedDeadLetterQueue.isPresent()) {
            declareAndBindDeadLetterQueue(channel, optionalPrefixedDeadLetterQueue.get());
            queueDeclarationArgs.put(X_DEAD_LETTER_ROUTING_KEY, context.getDeadLetterRoutingKey(queueName));
            queueDeclarationArgs.put(X_DEAD_LETTER_EXCHANGE, context.getDeadLetterExchangeName().orElseThrow());
        }

        // declare queue & exchange
        declareQueue(channel, consumerOnEveryInstance, queueName, queueDeclarationArgs);
        declareExchange(channel, exchange, exchangeType);

        // bind queues
        final var bindingKeys = getBindingKeys(exchangeType);
        for (String bindingKey : bindingKeys) {
            channel.queueBind(queueName, exchange, bindingKey);
        }

        // start consuming
        channel.basicConsume(queueName, false, this.callback,
                consumerTag -> log.warn("Channel canceled for {}", queueName),
                (consumerTag, sig) -> reInitChannel(sig, queueName, consumerTag));

        log.info("consumer started on queue '{}' --> {} binding key(s): {}", queueName, exchange,
                String.join(", ", bindingKeys));
    }

    private void reInitChannel(ShutdownSignalException sig, String queueName, String consumerTag) {
        log.warn("Channel shut down for with signal:{}, queue: {}, consumer: {}", sig, queueName, consumerTag);
        try {
            this.channelService.removeChannel(this.channelId);
            this.channelId = UUID.randomUUID().toString();
            initChannel();
        } catch (IOException e) {
            log.error(String.format("Could not re-initialize channel for queue %s", queueName), e);
        }
    }

    private void declareExchange(final Channel channel, final String exchange, final ExchangeType exchangeType)
            throws IOException {

        if (context.isFrontendMessage()) {
            channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, false);
        } else {
            final var type = BuiltinExchangeType.valueOf(exchangeType.name());
            channel.exchangeDeclare(exchange, type, true);
        }
    }

    private void declareQueue(final Channel channel, final boolean consumerOnEveryInstance, final String queueName,
            final Map<String, Object> args) throws IOException {

        final boolean durable = getDurable();
        final boolean autoDelete = context.isAutoDelete() && !consumerOnEveryInstance;
        try {
            AMQP.Queue.DeclareOk declareOk = channel.queueDeclare(queueName, durable, false, autoDelete, args);
            log.info("queue: {}, consumers: {}, message count: {}", declareOk.getQueue(), declareOk.getConsumerCount(),
                    declareOk.getMessageCount());
        } catch (IOException e) {
            long msgCount = channel.messageCount(queueName);
            if (msgCount <= 0) {
                channel.queueDelete(queueName, false, true);
                channel.queueDeclare(queueName, durable, false, autoDelete, args);
            } else {
                log.error("The new settings of queue was not set, because was not empty! queue={}", queueName, e);
            }
        }
    }

    private void declareAndBindDeadLetterQueue(final Channel channel, final String deadLetterQueue) throws IOException {
        if (context.isCustomDeadLetterQueue()) {
            channel.exchangeDeclare(deadLetterQueue, BuiltinExchangeType.TOPIC, true);
            channel.queueDeclare(deadLetterQueue, true, false, false, null);
            channel.queueBind(deadLetterQueue, deadLetterQueue, "#");
        }
    }

    private boolean getDurable() {
        if (context.isFrontendMessage()) {
            return false;
        }

        return context.isDurable();
    }

    private List<String> getBindingKeys(final ExchangeType exchangeType) {
        final var name = context.getName();
        if (context.isFrontendMessage()) {
            return List.of("#." + name);
        }

        return switch (exchangeType) {
            case DIRECT, TOPIC -> context.getBindingKeys();
            case FANOUT -> List.of("#." + name);
        };
    }

    private static void validateBindingKeys(final List<String> bindingKeys, final ExchangeType exchangeType) {
        if (exchangeType == ExchangeType.FANOUT) {
            return;
        }

        if (bindingKeys == null || bindingKeys.size() == 0) {
            throw new IllegalArgumentException("Binding key(s) are required when declaring a "
                    + exchangeType.name() + " type exchange.");
        }

        if (exchangeType == ExchangeType.DIRECT && bindingKeys.size() > 1) {
            throw new IllegalArgumentException("Exactly one binding key is required when declaring a direct type exchange.");
        }
    }
}
