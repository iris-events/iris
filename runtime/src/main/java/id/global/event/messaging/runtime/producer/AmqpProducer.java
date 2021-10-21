package id.global.event.messaging.runtime.producer;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import id.global.event.messaging.runtime.exception.AmqpSendException;

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

    public void send(final Object message) throws AmqpSendException, IOException {
        send(message, null);
    }

    public void send(final Object message, final MetadataInfo metadataInfo) throws AmqpSendException, IOException {
        if (message == null) {
            throw new AmqpSendException("Null message can not be published!");
        }

        Optional<ProducedEvent> producedEventMetadata = Optional.ofNullable(
                message.getClass().getAnnotation(ProducedEvent.class));
        Optional<EventMetadata> eventMetadata = Optional.ofNullable(message.getClass().getAnnotation(EventMetadata.class));

        final AMQP.BasicProperties amqpBasicProperties = getAmqpBasicProperties(metadataInfo);

        final var optionalExchange = getExchange(producedEventMetadata, eventMetadata);
        final var routingKey = getRoutingKey(producedEventMetadata, eventMetadata);
        final var exchangeType = getExchangeType(producedEventMetadata, eventMetadata);

        if (optionalExchange.isEmpty()) {
            throw new AmqpSendException("Could not send message to empty or null exchange.");
        }

        publish(message, optionalExchange.get(), routingKey, amqpBasicProperties, exchangeType);
    }

    public void send(final Object message, @NotNull final String exchange, final String routingKey,
            final ExchangeType exchangeType) throws AmqpSendException, IOException {

        AMQP.BasicProperties amqpBasicProperties = Optional.ofNullable(eventContext.getAmqpBasicProperties())
                .orElse(null);

        publishWithProperties(message, exchange, routingKey, exchangeType, amqpBasicProperties);
    }

    public void send(final Object message, @NotNull final String exchange, final String routingKey,
            final ExchangeType exchangeType, MetadataInfo metadataInfo) throws AmqpSendException, IOException {
        AMQP.BasicProperties amqpBasicProperties = getAmqpBasicProperties(metadataInfo);
        publishWithProperties(message, exchange, routingKey, exchangeType, amqpBasicProperties);
    }

    public void addReturnListener(@NotNull String channelKey, @NotNull ReturnListener returnListener) throws IOException {
        Objects.requireNonNull(returnListener, "Return listener can not be null");

        Channel channel = channelService.getOrCreateChannelById(channelKey);
        channel.clearReturnListeners();
        channel.addReturnListener(returnListener);
    }

    public void addReturnCallback(@NotNull String channelKey, @NotNull ReturnCallback returnCallback) throws IOException {
        Objects.requireNonNull(returnCallback, "Return callback can not be null");

        Channel channel = channelService.getOrCreateChannelById(channelKey);
        channel.clearReturnListeners();
        channel.addReturnListener(returnCallback);
    }

    public void addConfirmListener(@NotNull String channelKey, @NotNull ConfirmListener confirmListener) throws IOException {
        Objects.requireNonNull(confirmListener, "Confirm listener can not be null");

        Channel channel = channelService.getOrCreateChannelById(channelKey);
        channel.clearConfirmListeners();
        channel.addConfirmListener(confirmListener);
    }

    private void publish(@NotNull Object message, @NotNull String exchange, String routingKey, AMQP.BasicProperties properties,
            ExchangeType exchangeType)
            throws IOException, AmqpSendException {

        SendMessageValidator.validate(exchange, routingKey, exchangeType);

        final byte[] bytes = objectMapper.writeValueAsBytes(message);
        synchronized (this.lock) {
            String channelKey = Common.createChannelKey(exchange, routingKey);
            Channel channel = channelService.getOrCreateChannelById(channelKey);
            channel.basicPublish(exchange, routingKey, true, properties, bytes);

            if (shouldWaitForConfirmations()) {
                waitForConfirmations(channel);
            }
        }
    }

    private void publishWithProperties(final Object message, @NotNull final String exchange, final String routingKey,
            final ExchangeType exchangeType,
            final AMQP.BasicProperties amqpBasicProperties) throws AmqpSendException, IOException {
        String routingKeyO = Optional.ofNullable(routingKey).orElse("");
        ExchangeType exchangeTypeO = Optional.ofNullable(exchangeType).orElse(ExchangeType.DIRECT);

        publish(message, exchange, routingKeyO, amqpBasicProperties, exchangeTypeO);
    }

    private void waitForConfirmations(Channel channel) throws AmqpSendException {
        try {
            channel.waitForConfirms(WAIT_TIMEOUT_MILLIS);
        } catch (InterruptedException | TimeoutException e) {
            throw new AmqpSendException("Exception waiting for confirmations.", e);
        } finally {
            count.set(0);
        }
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
}
