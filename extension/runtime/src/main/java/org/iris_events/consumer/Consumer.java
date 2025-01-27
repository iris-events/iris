package org.iris_events.consumer;

import static org.iris_events.common.MessagingHeaders.QueueDeclaration.X_DEAD_LETTER_EXCHANGE;
import static org.iris_events.common.MessagingHeaders.QueueDeclaration.X_DEAD_LETTER_ROUTING_KEY;
import static org.iris_events.common.MessagingHeaders.QueueDeclaration.X_MESSAGE_TTL;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.context.IrisContext;
import org.iris_events.runtime.ExchangeNameProvider;
import org.iris_events.runtime.QueueNameProvider;
import org.iris_events.runtime.channel.ChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.rabbitmq.client.ShutdownSignalException;

public class Consumer implements RecoveryListener {
    private static final Logger log = LoggerFactory.getLogger(Consumer.class);
    private static final String RPC_EXCHANGE_SUFFIX = "rpc";

    private final IrisContext context;
    private final ChannelService channelService;
    private final DeliverCallbackProvider deliverCallbackProvider;
    private final QueueNameProvider queueNameProvider;
    private final ExchangeNameProvider exchangeNameProvider;
    private final QueueDeclarator queueDeclarator;
    private final ExchangeDeclarator exchangeDeclarator;

    private DeliverCallback callback;
    private String channelId;

    public Consumer(
            final IrisContext context,
            final ChannelService channelService,
            final DeliverCallbackProvider deliverCallbackProvider,
            final QueueNameProvider queueNameProvider,
            final ExchangeNameProvider exchangeNameProvider,
            final QueueDeclarator queueDeclarator,
            final ExchangeDeclarator exchangeDeclarator) {

        this.context = context;
        this.channelService = channelService;
        this.deliverCallbackProvider = deliverCallbackProvider;
        this.queueNameProvider = queueNameProvider;
        this.exchangeNameProvider = exchangeNameProvider;
        this.queueDeclarator = queueDeclarator;
        this.exchangeDeclarator = exchangeDeclarator;
        this.channelId = UUID.randomUUID().toString();
    }

    public void initChannel() throws IOException {
        final var channel = channelService.getOrCreateChannelById(this.channelId);
        this.callback = deliverCallbackProvider.createDeliverCallback(channel);
        final var exchangeType = context.exchangeType();
        validateBindingKeys(context.getBindingKeys(), exchangeType);
        declareTopology(channel, exchangeType);

        if (channel instanceof Recoverable) {
            ((Recoverable) channel).addRecoveryListener(this);
        }
    }

    public DeliverCallback getCallback() {
        return callback;
    }

    protected IrisContext getContext() {
        return context;
    }

    private void declareTopology(Channel channel, final ExchangeType exchangeType) throws IOException {
        final var queueDeclarationArgs = new HashMap<String, Object>();

        final var exchange = getExchangeName();
        final var queueName = queueNameProvider.getQueueName(context);

        // set prefetch count "quality of service"
        final int prefetchCount = context.getPrefetch();
        channel.basicQos(prefetchCount);

        // time to live of queue
        final long ttl = context.getTtl();
        if (ttl >= 0) {
            queueDeclarationArgs.put(X_MESSAGE_TTL, ttl);
        }

        if (context.isRpc()) {
            // TODO define TTL in config
            final var rpcTtl = 2000;
            queueDeclarationArgs.put(X_MESSAGE_TTL, rpcTtl);

            // Declare incoming / request queue and exchange and bind them
            final var rpcRequestExchange = exchangeNameProvider.getRpcRequestExchangeName(context.getName());
            final var rpcResponseExchange = exchangeNameProvider.getRpcResponseExchangeName(context.getRpcResponseEventName());

            log.info(String.format(
                    "Declaring topology for RPC consumer.\nrequestExchange: %s\nresponseExchange: %s\nrequestQueue: %s\n",
                    rpcRequestExchange, rpcResponseExchange, queueName));

            declareQueue(channel, queueName, false, true, queueDeclarationArgs);
            exchangeDeclarator.declareExchange(rpcRequestExchange, ExchangeType.FANOUT, false);
            channel.queueBind(queueName, rpcRequestExchange, queueName);

            // Also declare outgoing / response exchange. Event producers / clients should bind their queues here
            exchangeDeclarator.declareExchange(rpcResponseExchange, ExchangeType.DIRECT, false);

            // start consuming
            channel.basicConsume(queueName, false, this.callback,
                    consumerTag -> log.warn("Channel canceled for {}", queueName),
                    (consumerTag, sig) -> reInitChannel(sig, queueName, consumerTag));

            log.info("consumer (RPC) started on queue '{}' --> {} binding key(s): {}", queueName, exchange, queueName);
        } else {
            // setup dead letter queue
            final var optionalPrefixedDeadLetterQueue = context.getDeadLetterQueueName();
            if (optionalPrefixedDeadLetterQueue.isPresent()) {
                declareAndBindDeadLetterQueue(channel, optionalPrefixedDeadLetterQueue.get());
                queueDeclarationArgs.put(X_DEAD_LETTER_ROUTING_KEY, context.getDeadLetterRoutingKey(queueName));
                queueDeclarationArgs.put(X_DEAD_LETTER_EXCHANGE, context.getDeadLetterExchangeName().orElseThrow());
            }

            // declare queue & exchange
            final boolean autoDelete = context.isConsumerOnEveryInstance() || context.isAutoDelete();
            declareQueue(channel, queueName, getDurable(), autoDelete, queueDeclarationArgs);
            exchangeDeclarator.declareExchange(exchange, exchangeType, context.isFrontendMessage());

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
    }

    private String getExchangeName() {
        return context.getName();
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

    private void declareQueue(final Channel channel, final String queueName, final boolean durable, final boolean autoDelete,
            final Map<String, Object> args) throws IOException {
        final var details = new QueueDeclarator.QueueDeclarationDetails(queueName, durable, false, autoDelete, args);
        queueDeclarator.declareQueueWithRecreateOnConflict(channel, details);
    }

    private void declareAndBindDeadLetterQueue(final Channel channel, final String deadLetterQueue) throws IOException {
        if (context.isCustomDeadLetterQueue()) {
            channel.exchangeDeclare(deadLetterQueue, BuiltinExchangeType.TOPIC, true);
            final var details = new QueueDeclarator.QueueDeclarationDetails(deadLetterQueue, true, false, false, null);
            queueDeclarator.declareQueueWithRecreateOnConflict(channel, details);
            channel.queueBind(deadLetterQueue, deadLetterQueue, "#");
        }
    }

    private boolean getDurable() {
        if (context.isFrontendMessage()) {
            return false;
        }
        if (context.isRpc()) {
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

    @Override
    public void handleRecovery(Recoverable recoverable) {
        log.info("handleRecovery called for consumer {}", context.getName());
    }

    @Override
    public void handleRecoveryStarted(Recoverable recoverable) {
        log.info("handleRecoveryStarted for consumer {}", context.getName());
    }
}
