package id.global.iris.messaging.runtime.consumer;

import static id.global.iris.common.constants.MessagingHeaders.QueueDeclaration.X_MESSAGE_TTL;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerShutdownSignalCallback;
import com.rabbitmq.client.DeliverCallback;

import id.global.iris.common.constants.Exchanges;
import id.global.iris.messaging.runtime.InstanceInfoProvider;
import id.global.iris.messaging.runtime.QueueNameProvider;
import id.global.iris.messaging.runtime.channel.ChannelService;
import id.global.iris.messaging.runtime.exception.AmqpConnectionException;

@ApplicationScoped
public class FrontendAmqpConsumer {
    private static final Logger log = LoggerFactory.getLogger(FrontendAmqpConsumer.class);

    private static final int DEFAULT_MESSAGE_TTL = 15000;

    private final ChannelService channelService;
    private final InstanceInfoProvider instanceInfoProvider;
    private final QueueDeclarator queueDeclarator;
    private final ConcurrentHashMap<String, DeliverCallbackProvider> deliverCallbackProviderMap;
    private final ConcurrentHashMap<String, DeliverCallback> deliverCallbackMap;
    private final String queueName;
    private String channelId;

    @Inject
    public FrontendAmqpConsumer(
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
        } catch (IOException e) {
            String msg = "Could not initialize frontend consumer";
            log.error(msg, e);
            throw new AmqpConnectionException(msg, e);
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
                throw new AmqpConnectionException(msg, e);
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
}
