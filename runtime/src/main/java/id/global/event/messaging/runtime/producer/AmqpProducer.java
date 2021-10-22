package id.global.event.messaging.runtime.producer;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
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
import id.global.event.messaging.runtime.exception.AmqpTransactionException;
import id.global.event.messaging.runtime.exception.AmqpTransactionRuntimeException;
import id.global.event.messaging.runtime.tx.TransactionCallback;

@ApplicationScoped
public class AmqpProducer {
    private static final Logger LOG = LoggerFactory.getLogger(AmqpProducer.class);
    private static final long WAIT_TIMEOUT_MILLIS = 2000;

    private final ProducerChannelService channelService;
    private final ObjectMapper objectMapper;
    private final EventContext eventContext;
    private final AmqpConfiguration configuration;
    private final TransactionManager transactionManager;

    private final AtomicInteger count = new AtomicInteger(0);
    private final Object lock = new Object();

    private final Map<Transaction, List<Message>> transactionDelayedMessages;

    private TransactionCallback transactionCallback;

    @Inject
    public AmqpProducer(ProducerChannelService channelService, ObjectMapper objectMapper,
            EventContext eventContext, AmqpConfiguration configuration, TransactionManager transactionManager) {
        this.channelService = channelService;
        this.objectMapper = objectMapper;
        this.eventContext = eventContext;
        this.configuration = configuration;
        this.transactionManager = transactionManager;

        this.transactionDelayedMessages = new HashMap<>();
    }

    public void send(final Object message) throws AmqpSendException, AmqpTransactionException {
        send(message, null);
    }

    public void send(final Object message, final MetadataInfo metadataInfo) throws AmqpSendException, AmqpTransactionException {
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
            final ExchangeType exchangeType) throws AmqpSendException, AmqpTransactionException {

        AMQP.BasicProperties amqpBasicProperties = Optional.ofNullable(eventContext.getAmqpBasicProperties())
                .orElse(null);

        publishWithProperties(message, exchange, routingKey, exchangeType, amqpBasicProperties);
    }

    public void send(final Object message, @NotNull final String exchange, final String routingKey,
            final ExchangeType exchangeType, MetadataInfo metadataInfo) throws AmqpSendException, AmqpTransactionException {
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

    public void registerTransactionCallback(TransactionCallback callback) {
        this.transactionCallback = callback;
    }

    private void publish(@NotNull Object message, @NotNull String exchange, String routingKey, AMQP.BasicProperties properties,
            ExchangeType exchangeType) throws AmqpSendException, AmqpTransactionException {

        SendMessageValidator.validate(exchange, routingKey, exchangeType);
        final var txOptional = getOptionalTransaction();

        if (txOptional.isPresent()) {
            final var tx = txOptional.get();
            enqueueDelayedMessage(message, exchange, routingKey, properties, tx);
            registerDefaultTransactionCallback(tx);
        } else {
            executePublish(message, exchange, routingKey, properties);
        }
    }

    private void enqueueDelayedMessage(Object message, String exchange, String routingKey, AMQP.BasicProperties properties,
            Transaction tx) {
        transactionDelayedMessages.computeIfAbsent(tx, k -> new LinkedList<>());
        transactionDelayedMessages.get(tx).add(new Message(message, exchange, routingKey, properties));
    }

    private Optional<Transaction> getOptionalTransaction() throws AmqpTransactionException {
        try {
            return Optional.ofNullable(transactionManager.getTransaction());
        } catch (SystemException e) {
            throw new AmqpTransactionException("Exception retrieving transaction from transaction manager", e);
        }
    }

    private void registerDefaultTransactionCallback(Transaction tx) throws AmqpSendException {
        try {
            tx.registerSynchronization(new AmqpProducerSynchronization(tx));
        } catch (RollbackException | SystemException e) {
            throw new AmqpSendException("Exception registering transaction callback", e);
        }
    }

    private void executePublish(List<Message> delayedMessageList) throws IOException, AmqpSendException {
        LinkedList<Message> messageList = (LinkedList<Message>) delayedMessageList;
        Message message = messageList.poll();

        while (message != null) {
            executePublish(message.message(), message.exchange(), message.routingKey(),
                    message.properties());
            message = messageList.poll();
        }
    }

    private void executePublish(Object message, String exchange, String routingKey, AMQP.BasicProperties properties)
            throws AmqpSendException {

        try {
            final byte[] bytes = objectMapper.writeValueAsBytes(message);
            synchronized (this.lock) {
                String channelKey = Common.createChannelKey(exchange, routingKey);
                Channel channel = channelService.getOrCreateChannelById(channelKey);
                channel.basicPublish(exchange, routingKey, true, properties, bytes);

                if (shouldWaitForConfirmations()) {
                    waitForConfirmations(channel);
                }
            }

        } catch (IOException e) {
            throw new AmqpSendException("Exception executing publish.", e);
        }
    }

    private void publishWithProperties(final Object message, @NotNull final String exchange, final String routingKey,
            final ExchangeType exchangeType,
            final AMQP.BasicProperties amqpBasicProperties) throws AmqpSendException, AmqpTransactionException {
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

    private class AmqpProducerSynchronization implements Synchronization {
        private final Transaction tx;

        public AmqpProducerSynchronization(Transaction tx) {
            this.tx = tx;
        }

        @Override
        public void beforeCompletion() {
            try {
                boolean isCallbackPresent = transactionCallback != null;
                if (isCallbackPresent) {
                    transactionCallback.beforeTxPublish(transactionDelayedMessages.get(tx));
                }
                executePublish(transactionDelayedMessages.get(tx));

                if (isCallbackPresent) {
                    transactionCallback.afterTxPublish();
                }
            } catch (IOException | AmqpSendException e) {
                LOG.error("Exception completing send transaction.", e);
                throw new AmqpTransactionRuntimeException("Exception completing send transaction");
            }
        }

        @Override
        public void afterCompletion(int status) {
            // This executes after commit AND rollback, for now just remove the TX messages
            if (transactionCallback != null) {
                transactionCallback.afterTxCompletion(transactionDelayedMessages.get(tx), status);
            }
            transactionDelayedMessages.remove(tx);
        }
    }
}
