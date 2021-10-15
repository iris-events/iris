package id.global.event.messaging.runtime.producer;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.ReturnCallback;
import com.rabbitmq.client.ReturnListener;

import id.global.asyncapi.spec.annotations.ProducedEvent;
import id.global.asyncapi.spec.enums.ExchangeType;
import id.global.common.annotations.EventMetadata;
import id.global.event.messaging.runtime.Common;
import id.global.event.messaging.runtime.channel.ProducerChannelService;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.context.EventContext;

@ApplicationScoped
public class AmqpProducer {
    private static final Logger LOG = LoggerFactory.getLogger(AmqpProducer.class);
    private static final long WAIT_TIMEOUT_MILLIS = 2000;

    private final ProducerChannelService channelService;
    private final ObjectMapper objectMapper;
    private final EventContext eventContext;
    private final AmqpConfiguration configuration;

    private final AtomicInteger count = new AtomicInteger(0);
    private final Object lock = new Object();

    @Inject
    public AmqpProducer(ProducerChannelService channelService, ObjectMapper objectMapper,
            EventContext eventContext, AmqpConfiguration configuration) {
        this.channelService = channelService;
        this.objectMapper = objectMapper;
        this.eventContext = eventContext;
        this.configuration = configuration;
    }

    public boolean publish(final Object message) {
        return publish(message, null);
    }

    public boolean publish(final Object message, final MetadataInfo metadataInfo) {
        if (message == null) {
            LOG.warn("Null message cannot be published!");
            return false;
        }

        Optional<ProducedEvent> producedEventMetadata = Optional.ofNullable(
                message.getClass().getAnnotation(ProducedEvent.class));
        Optional<EventMetadata> eventMetadata = Optional.ofNullable(message.getClass().getAnnotation(EventMetadata.class));

        final AMQP.BasicProperties amqpBasicProperties = getAmqpBasicProperties(metadataInfo);

        final var optionalExchange = getExchange(producedEventMetadata, eventMetadata);
        final var routingKey = getRoutingKey(producedEventMetadata, eventMetadata);
        final var exchangeType = getExchangeType(producedEventMetadata, eventMetadata);

        if (optionalExchange.isEmpty()) {
            LOG.warn("Could not publish message with empty or null exchange");
            return false;
        }

        return routePublish(optionalExchange.get(), routingKey, exchangeType, message,
                amqpBasicProperties);
    }

    public boolean publish(final Object message, @NotNull final String exchange, final String routingKey,
            final ExchangeType exchangeType) {

        AMQP.BasicProperties amqpBasicProperties = Optional.ofNullable(eventContext.getAmqpBasicProperties())
                .orElse(null);

        return publishWithProperties(message, exchange, routingKey, exchangeType, amqpBasicProperties);
    }

    public boolean publish(final Object message, @NotNull final String exchange, final String routingKey,
            final ExchangeType exchangeType,
            MetadataInfo metadataInfo) {
        AMQP.BasicProperties amqpBasicProperties = getAmqpBasicProperties(metadataInfo);
        return publishWithProperties(message, exchange, routingKey, exchangeType, amqpBasicProperties);
    }

    public boolean addReturnListener(String channelKey, ReturnListener returnListener, ReturnCallback returnCallback)
            throws IOException {
        Channel channel = channelService.getChannel(channelKey);

        if (channel == null) {
            LOG.error("Cannot add return listeners as channel does not exist! channelKey=[{}]", channelKey);
            return false;
        }

        if (returnListener == null && returnCallback == null) {
            LOG.error("Cannot add return listeners no return listener was provided!");
            return false;
        }

        channel.clearReturnListeners();
        if (returnListener != null) {
            channel.addReturnListener(returnListener);
        }
        if (returnCallback != null) {
            channel.addReturnListener(returnCallback);
        }
        return true;
    }

    public boolean addConfirmListeners(String channelKey, ConfirmListener confirmListener) throws IOException {
        Channel channel = channelService.getChannel(channelKey);

        if (channel == null) {
            LOG.error("Cannot add confirm listeners as channel does not exist! channelKey=[{}]", channelKey);
            return false;
        }

        if (confirmListener == null) {
            LOG.error("Cannot add confirm listeners as confirm listener was not provided!");
            return false;
        }

        channel.clearConfirmListeners();
        channel.addConfirmListener(confirmListener);

        return true;
    }

    private void publish(final String exchange, final String routingKey, final AMQP.BasicProperties properties,
            final byte[] bytes,
            final Channel channel) throws IOException {
        if (channel == null) {
            throw new IOException("Publish failed! Channel does not exist!");
        }
        channel.basicPublish(exchange, routingKey, true, properties, bytes);
    }

    // TODO this method is quite useless, check calling hierarchy
    private boolean routePublish(@NotNull String exchange, String routingKey, @NotNull ExchangeType type,
            @NotNull Object message,
            AMQP.BasicProperties properties) {
        switch (type) {
            case TOPIC -> {
                return publishTopic(exchange, routingKey, message, properties);
            }
            case DIRECT -> {
                return publishDirect(exchange, routingKey, message, properties);
            }
            case FANOUT -> {
                return publishFanout(exchange, message, properties);
            }
            default -> {
                LOG.warn("Exchange type={} unknown! Message will be lost!", type);
                return false;
            }
        }
    }

    private boolean publishWithProperties(final Object message, final String exchange, final String routingKey,
            final ExchangeType exchangeType,
            final AMQP.BasicProperties amqpBasicProperties) {

        String exchangeO = Optional.ofNullable(exchange).orElse("");
        String routingKeyO = Optional.ofNullable(routingKey).orElse("");
        ExchangeType exchangeTypeO = Optional.ofNullable(exchangeType).orElse(ExchangeType.DIRECT);

        if (exchangeO.isBlank()) {
            return false;
        }

        return routePublish(exchangeO, routingKeyO, exchangeTypeO, message,
                amqpBasicProperties);
    }

    private boolean publishDirect(String exchange, String routingKey, Object message,
            AMQP.BasicProperties properties) {
        if (isNullOrEmpty(exchange)) {
            LOG.warn("Could not publish message to DIRECT exchange with empty exchange parameter.");
            return false;
        }

        if (isNullOrEmpty(routingKey)) {
            LOG.warn("Could not publish message to DIRECT exchange with empty routingKey parameter.");
            return false;
        }

        try {
            final byte[] bytes = objectMapper.writeValueAsBytes(message);
            return publishMessage(exchange,
                    routingKey,
                    properties,
                    bytes);
        } catch (JsonProcessingException e) {
            LOG.error("Sending message to exchange=[{}] with routingKey=[{}] failed!", exchange, routingKey, e);
            return false;
        }
    }

    private boolean publishTopic(String exchange, String topicRoutingKey, Object message, AMQP.BasicProperties properties) {
        if (isNullOrEmpty(exchange)) {
            LOG.warn("Could not publish message to TOPIC exchange with empty exchange parameter.");
            return false;
        }
        if (isNullOrEmpty(topicRoutingKey)) {
            LOG.warn("Could not publish message to TOPIC exchange with empty routingKey parameter.");
            return false;
        }

        try {
            final byte[] bytes = objectMapper.writeValueAsBytes(message);
            return publishMessage(exchange,
                    topicRoutingKey,
                    properties,
                    bytes);
        } catch (JsonProcessingException e) {
            LOG.error("Sending message to exchange=[{}] with routingKey=[{}] failed!", exchange, topicRoutingKey, e);
            return false;
        }
    }

    private boolean publishFanout(String fanoutExchange, Object message, AMQP.BasicProperties properties) {
        if (isNullOrEmpty(fanoutExchange)) {
            LOG.warn("Could not publish message to FANOUT exchange with empty exchange parameter.");
            return false;
        }

        try {
            final byte[] bytes = objectMapper.writeValueAsBytes(message);
            return publishMessage(fanoutExchange,
                    "",
                    properties,
                    bytes);
        } catch (JsonProcessingException e) {
            LOG.error("Sending to fanout exchange=[{}]] failed! ", fanoutExchange, e);
            return false;
        }
    }

    private boolean publishMessage(final String exchange,
            final String routingKey,
            final AMQP.BasicProperties properties,
            final byte[] bytes) {
        synchronized (this.lock) {
            String channelKey = Common.createChannelKey(exchange, routingKey);
            try {
                Channel channel = channelService.getChannel(channelKey);
                publish(exchange, routingKey, properties, bytes, channel);

                if (shouldWaitForConfirmations()) {
                    waitForConfirmations(channel);
                }
            } catch (IOException e) {
                LOG.error("Exception while publishing message", e);
                return false;
            }
            return true;
        }
    }

    private void waitForConfirmations(Channel channel) {
        try {
            channel.waitForConfirms(WAIT_TIMEOUT_MILLIS);
        } catch (InterruptedException | TimeoutException e) {
            LOG.error("Waiting for channel confirmations failed.", e);
            // TODO this should be properly handled with a custom exeption and the client should then decide what to do in this case
        }
        count.set(0);
    }

    private AMQP.BasicProperties getAmqpBasicProperties(MetadataInfo metadataInfo) {
        return Optional.ofNullable(metadataInfo)
                .map(info -> new AMQP.BasicProperties().builder()
                        .correlationId(info.correlationId())
                        .build())
                .or(() -> Optional.ofNullable(eventContext.getAmqpBasicProperties())).orElse(null);
    }

    private ExchangeType getExchangeType(Optional<ProducedEvent> producedEventMetadata, Optional<EventMetadata> eventMetadata) {
        return eventMetadata.map(metadata -> ExchangeType.fromType(metadata.exchangeType().toUpperCase()))
                .or(() -> producedEventMetadata.map(ProducedEvent::exchangeType)).orElse(ExchangeType.DIRECT);
    }

    private String getRoutingKey(Optional<ProducedEvent> producedEventMetadata,
            Optional<EventMetadata> eventMetadata) {
        return eventMetadata.map(EventMetadata::routingKey).or(() -> producedEventMetadata.map(ProducedEvent::queue))
                .orElse(null);
    }

    private Optional<String> getExchange(Optional<ProducedEvent> producedEventMetadata, Optional<EventMetadata> eventMetadata) {
        return eventMetadata.map(EventMetadata::exchange)
                .or(() -> producedEventMetadata.map(ProducedEvent::exchange));
    }

    private boolean shouldWaitForConfirmations() {
        return configuration.getConfirmationBatchSize() > 0
                && count.incrementAndGet() == configuration.getConfirmationBatchSize();
    }

    private boolean isNullOrEmpty(String string) {
        return string == null || string.isBlank();
    }
}
