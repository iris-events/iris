package org.iris_events.consumer;

import static org.iris_events.common.MessagingHeaders.QueueDeclaration.X_MESSAGE_TTL;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.iris_events.common.Exchanges;
import org.iris_events.exception.IrisConnectionException;
import org.iris_events.runtime.InstanceInfoProvider;
import org.iris_events.runtime.QueueNameProvider;
import org.iris_events.runtime.channel.ChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerShutdownSignalCallback;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;

@ApplicationScoped
public class FrontendEventConsumer implements RecoveryListener {
    private static final Logger log = LoggerFactory.getLogger(FrontendEventConsumer.class);

    private static final int DEFAULT_MESSAGE_TTL = 15000;

    private final ChannelService channelService;
    private final InstanceInfoProvider instanceInfoProvider;
    private final QueueDeclarator queueDeclarator;
    private final ConcurrentHashMap<String, DeliverCallbackProvider> deliverCallbackProviderMap;
    private final ConcurrentHashMap<String, DeliverCallback> deliverCallbackMap;
    private final String queueName;
    private String channelId;

    @Inject
    public FrontendEventConsumer(
            @Named("consumerChannelService") final ChannelService channelService,
            final InstanceInfoProvider instanceInfoProvider,
            final QueueDeclarator queueDeclarator,
            final QueueNameProvider queueNameProvider) {
        this.channelService = channelService;
        this.instanceInfoProvider = instanceInfoProvider;
        this.queueDeclarator = queueDeclarator;
        this.deliverCallbackMap = new ConcurrentHashMap<>();
        this.deliverCallbackProviderMap = new ConcurrentHashMap<>();
        this.queueName = queueNameProvider.getFrontendQueueName();
        this.channelId = UUID.randomUUID().toString();
    }

    public void addDeliverCallbackProvider(String routingKey, DeliverCallbackProvider deliverCallbackProvider) {
        deliverCallbackProviderMap.put(routingKey, deliverCallbackProvider);
    }

    public void initChannel() {
        try {
            Channel channel = this.channelService.getOrCreateChannelById(this.channelId);
            String frontendQueue = queueName;
            final var args = new HashMap<String, Object>();
            args.put(X_MESSAGE_TTL, DEFAULT_MESSAGE_TTL);
            final var details = new QueueDeclarator.QueueDeclarationDetails(frontendQueue, true, false, false, args);
            queueDeclarator.declareQueueWithRecreateOnConflict(channel, details);

            setupDeliverCallbacks(channel);

            channel.basicConsume(frontendQueue, false, getDeliverCallback(), getCancelCallback(), getShutdownCallback());

            if (channel instanceof Recoverable) {
                ((Recoverable) channel).addRecoveryListener(this);
            }
        } catch (IOException e) {
            String msg = "Could not initialize frontend consumer";
            log.error(msg, e);
            throw new IrisConnectionException(msg, e);
        }

    }

    private void setupDeliverCallbacks(Channel channel) {
        deliverCallbackProviderMap.forEach((routingKey, callbackProvider) -> {
            try {
                channel.queueBind(queueName, Exchanges.FRONTEND.getValue(), routingKey);
                deliverCallbackMap.put(routingKey, callbackProvider.createDeliverCallback(channel));
            } catch (IOException e) {
                String msg = String.format("Could not setup deliver callback for routing key = %s", routingKey);
                log.error(msg);
                throw new IrisConnectionException(msg, e);
            }
        });
    }

    private DeliverCallback getDeliverCallback() {
        return (consumerTag, message) -> {
            String msgRoutingKey = message.getEnvelope().getRoutingKey();

            DeliverCallback deliverCallback = deliverCallbackMap.get(msgRoutingKey);
            if (deliverCallback == null) {
                log.warn(String.format("No handler registered for frontend message with routingKey = %s, NACK-ing message",
                        msgRoutingKey));
                channelService.getOrCreateChannelById(channelId)
                        .basicNack(message.getEnvelope().getDeliveryTag(), false, false);
            } else {
                deliverCallback.handle(consumerTag, message);
            }
        };
    }

    private CancelCallback getCancelCallback() {
        return consumerTag -> log.warn("Channel canceled for {}",
                instanceInfoProvider.getApplicationName() + " frontend queue");
    }

    private ConsumerShutdownSignalCallback getShutdownCallback() {
        return (consumerTag, sig) -> {
            log.warn("Channel shut down for with signal:{}, queue: {}, consumer: {}", sig, queueName, consumerTag);
            try {
                channelService.removeChannel(channelId);
                channelId = UUID.randomUUID().toString();
                initChannel();
            } catch (IOException e) {
                log.error(String.format("Could not re-initialize channel for queue %s", queueName), e);
            }
        };
    }

    @Override
    public void handleRecovery(Recoverable recoverable) {
        log.info("handleRecovery called for frontend consumer for queue {}", queueName);
        initChannel();
    }

    @Override
    public void handleRecoveryStarted(Recoverable recoverable) {
        log.info("handleRecoveryStarted called for frontend consumer for queue {}", queueName);
    }
}
