package org.iris_events.producer;

import static org.iris_events.common.constants.Exchanges.BROADCAST;
import static org.iris_events.common.constants.Exchanges.SESSION;
import static org.iris_events.common.constants.Exchanges.SUBSCRIPTION;
import static org.iris_events.common.constants.Exchanges.USER;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import org.iris_events.runtime.configuration.IrisRabbitMQConfig;
import org.iris_events.exception.IrisSendException;
import org.iris_events.exception.IrisTransactionException;
import org.iris_events.tx.TransactionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.ReturnCallback;
import com.rabbitmq.client.ReturnListener;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.annotations.Scope;
import org.iris_events.common.message.ResourceMessage;
import org.iris_events.runtime.BasicPropertiesProvider;
import org.iris_events.runtime.channel.ChannelKey;
import org.iris_events.runtime.channel.ChannelService;
import org.iris_events.context.EventContext;
import org.iris_events.asyncapi.parsers.ExchangeParser;
import org.iris_events.asyncapi.parsers.ExchangeTypeParser;
import org.iris_events.asyncapi.parsers.MessageScopeParser;
import org.iris_events.asyncapi.parsers.PersistentParser;
import org.iris_events.asyncapi.parsers.RoutingKeyParser;
import jakarta.validation.constraints.NotNull;

@ApplicationScoped
public class EventProducer {
    private static final Logger log = LoggerFactory.getLogger(EventProducer.class);

    public static final String SERVICE_ID_UNAVAILABLE_FALLBACK = "N/A";
    private static final long WAIT_TIMEOUT_MILLIS = 2000;
    private static final String RESOURCE = "resource";

    private final ChannelService channelService;
    private final ObjectMapper objectMapper;
    private final EventContext eventContext;
    private final IrisRabbitMQConfig config;
    private final TransactionManager transactionManager;
    private final BasicPropertiesProvider basicPropertiesProvider;

    private final AtomicInteger count = new AtomicInteger(0);
    private final Object lock = new Object();

    private final Map<Transaction, List<Message>> transactionDelayedMessages = new ConcurrentHashMap<>();

    private TransactionCallback transactionCallback;

    @Inject
    public EventProducer(@Named("producerChannelService") ChannelService channelService,
            ObjectMapper objectMapper,
            EventContext eventContext,
            IrisRabbitMQConfig config,
            TransactionManager transactionManager,
            BasicPropertiesProvider basicPropertiesProvider) {
        this.channelService = channelService;
        this.objectMapper = objectMapper;
        this.eventContext = eventContext;
        this.config = config;
        this.transactionManager = transactionManager;
        this.basicPropertiesProvider = basicPropertiesProvider;
    }

    /**
     * Send message using Iris infrastructure.
     *
     * @param message message
     * @throws IrisSendException when sending fails
     * @throws IrisTransactionException when sending fails within transactional context
     */
    public void send(final Object message) throws IrisSendException, IrisTransactionException {
        doSend(message, null);
    }

    /**
     * Send message using Iris infrastructure and override userId header of the message
     * All following events caused by this event will have that user id set and any client scoped messages will be sent to that
     * user.
     * <p>
     * Used to address specific user without FRONTED event being sent fom the client.
     * Scope of current event will be changed to USER if event is of client scope (SESSION, USER, BROADCAST).
     *
     * @param message message
     * @param userId user id
     * @throws IrisSendException when sending fails
     * @throws IrisTransactionException when sending fails within transactional context
     */
    public void send(final Object message, final String userId) throws IrisSendException, IrisTransactionException {
        doSend(message, userId);
    }

    /**
     * Send message to Iris subscription service.
     *
     * @param message message
     * @param resourceType resource type
     * @param resourceId resource id
     * @throws IrisSendException when sending fails
     * @throws IrisTransactionException when sending fails within transactional context
     */
    public void sendToSubscription(final Object message, final String resourceType, final String resourceId)
            throws IrisSendException, IrisTransactionException {
        final var messageAnnotation = getMessageAnnotation(message);

        if (resourceType == null || resourceType.isBlank()) {
            throw new IrisSendException("Resource type is required for subscription event!");
        }

        final var eventName = ExchangeParser.getFromAnnotationClass(messageAnnotation);
        final var routingKey = String.format("%s.%s", eventName, RESOURCE);
        final var resourceUpdate = new ResourceMessage(resourceType, resourceId, message);
        final var persistent = PersistentParser.getFromAnnotationClass(messageAnnotation);

        final var routingDetails = new RoutingDetails(eventName, SUBSCRIPTION.getValue(), ExchangeType.TOPIC, routingKey, null,
                null, null, null, persistent);
        publish(resourceUpdate, routingDetails);
    }

    private void doSend(final Object message, final String userId) throws IrisSendException {
        final var messageAnnotation = getMessageAnnotation(message);

        final var scope = MessageScopeParser.getFromAnnotationClass(messageAnnotation);

        switch (scope) {
            case INTERNAL -> publish(message, getRoutingDetailsFromAnnotation(messageAnnotation, scope, userId));
            case USER, SESSION, BROADCAST -> publish(message,
                    getRoutingDetailsForClientScope(messageAnnotation, scope, userId));
            default -> throw new IrisSendException("Message scope " + scope + " not supported!");
        }
    }

    private RoutingDetails getRoutingDetailsFromAnnotation(final org.iris_events.annotations.Message messageAnnotation,
            final Scope scope, final String userId) {

        final var exchangeType = ExchangeTypeParser.getFromAnnotationClass(messageAnnotation);
        final var eventName = ExchangeParser.getFromAnnotationClass(messageAnnotation);
        final var routingKey = getRoutingKey(messageAnnotation, exchangeType);
        final var persistent = PersistentParser.getFromAnnotationClass(messageAnnotation);

        return new RoutingDetails(eventName, eventName, exchangeType, routingKey, scope, userId, null, null, persistent);
    }

    private RoutingDetails getRoutingDetailsForClientScope(final org.iris_events.annotations.Message messageAnnotation,
            final Scope scope, final String userId) {

        final var exchange = Optional.ofNullable(userId)
                .map(u -> USER.getValue())
                .orElseGet(() -> switch (scope) {
                    case USER -> USER.getValue();
                    case SESSION -> SESSION.getValue();
                    case BROADCAST -> BROADCAST.getValue();
                    default -> throw new IrisSendException("Message scope " + scope + " not supported!");
                });

        final var eventName = ExchangeParser.getFromAnnotationClass(messageAnnotation);
        final var routingKey = String.format("%s.%s", eventName, exchange);
        final var persistent = PersistentParser.getFromAnnotationClass(messageAnnotation);

        return new RoutingDetails(eventName, exchange, ExchangeType.TOPIC, routingKey, scope, userId, null, null, persistent);
    }

    private org.iris_events.annotations.Message getMessageAnnotation(final Object message) {
        if (message == null) {
            throw new IrisSendException("Null message can not be published!");
        }

        return Optional
                .ofNullable(message.getClass().getAnnotation(org.iris_events.annotations.Message.class))
                .orElseThrow(() -> new IrisSendException("Message annotation is required."));
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

    private void publish(@NotNull Object message, RoutingDetails routingDetails)
            throws IrisSendException {

        SendMessageValidator.validate(routingDetails);
        final var txOptional = getOptionalTransaction();

        if (txOptional.isPresent()) {
            final var tx = txOptional.get();
            enqueueDelayedMessage(message, routingDetails, tx);
            registerDefaultTransactionCallback(tx);
        } else {
            executePublish(message, routingDetails);
        }
    }

    private void enqueueDelayedMessage(Object message, RoutingDetails routingDetails, Transaction tx) {
        final var properties = basicPropertiesProvider.getOrCreateAmqpBasicProperties(routingDetails);
        transactionDelayedMessages.computeIfAbsent(tx, k -> new LinkedList<>())
                .add(new Message(message, routingDetails, properties, eventContext.getEnvelope()));
    }

    private Optional<Transaction> getOptionalTransaction() throws IrisTransactionException {
        try {
            return Optional.ofNullable(transactionManager.getTransaction());
        } catch (SystemException e) {
            throw new IrisTransactionException("Exception retrieving transaction from transaction manager", e);
        }
    }

    private void registerDefaultTransactionCallback(Transaction tx) throws IrisSendException {
        try {
            tx.registerSynchronization(new ProducerSynchronization(tx));
        } catch (RollbackException | SystemException e) {
            throw new IrisSendException("Exception registering transaction callback", e);
        }
    }

    private void executeTxPublish(Transaction transaction) throws IOException, IrisSendException {
        LinkedList<Message> messageList = (LinkedList<Message>) transactionDelayedMessages.get(transaction);
        Message message = messageList.poll();

        while (message != null) {
            eventContext.setEnvelope(message.envelope());
            eventContext.setBasicProperties(message.properties());
            executePublish(message.message(), message.routingDetails());
            message = messageList.poll();
        }
    }

    private void executePublish(Object message, RoutingDetails routingDetails) throws IrisSendException {
        final var exchange = routingDetails.exchange();
        final var routingKey = routingDetails.routingKey();

        try {
            final byte[] bytes = objectMapper.writeValueAsBytes(message);
            synchronized (this.lock) {
                final var properties = basicPropertiesProvider.getOrCreateAmqpBasicProperties(routingDetails);
                final var channelKey = ChannelKey.create(exchange, routingKey);
                final var channel = channelService.getOrCreateChannelById(channelKey);
                log.info("publishing event to exchange: {}, routing key: {}, props: {}"
                        + "", exchange, routingKey, properties);
                channel.basicPublish(exchange, routingKey, true, properties, bytes);

                if (shouldWaitForConfirmations()) {
                    waitForConfirmations(channel);
                }
            }

        } catch (IOException e) {
            throw new IrisSendException("Exception executing publish.", e);
        }
    }

    private void waitForConfirmations(Channel channel) throws IrisSendException {
        try {
            channel.waitForConfirms(WAIT_TIMEOUT_MILLIS);
        } catch (InterruptedException | TimeoutException e) {
            throw new IrisSendException("Exception waiting for confirmations.", e);
        } finally {
            count.set(0);
        }
    }

    private String getRoutingKey(org.iris_events.annotations.Message messageAnnotation,
                                 final ExchangeType exchangeType) {
        if (exchangeType == ExchangeType.FANOUT) {
            return "";
        }

        return RoutingKeyParser.getFromAnnotationClass(messageAnnotation);
    }

    private boolean shouldWaitForConfirmations() {
        return config.getConfirmationBatchSize() > 0
                && count.incrementAndGet() == config.getConfirmationBatchSize();
    }

    private class ProducerSynchronization implements Synchronization {
        private final Transaction tx;

        public ProducerSynchronization(Transaction tx) {
            this.tx = tx;
        }

        @Override
        public void beforeCompletion() {
            try {
                boolean isCallbackPresent = transactionCallback != null;
                if (isCallbackPresent) {
                    transactionCallback.beforeTxPublish(transactionDelayedMessages.get(tx));
                }
                executeTxPublish(tx);

                if (isCallbackPresent) {
                    transactionCallback.afterTxPublish();
                }
            } catch (IOException | IrisSendException e) {
                log.error("Exception completing send transaction.", e);
                throw new IrisTransactionException("Exception completing send transaction");
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
