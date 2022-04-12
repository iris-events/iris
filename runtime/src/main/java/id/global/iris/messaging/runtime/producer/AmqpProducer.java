package id.global.iris.messaging.runtime.producer;

import static id.global.common.constants.iris.Exchanges.BROADCAST;
import static id.global.common.constants.iris.Exchanges.SESSION;
import static id.global.common.constants.iris.Exchanges.USER;
import static id.global.common.constants.iris.MessagingHeaders.Message.CURRENT_SERVICE_ID;
import static id.global.common.constants.iris.MessagingHeaders.Message.EVENT_TYPE;
import static id.global.common.constants.iris.MessagingHeaders.Message.INSTANCE_ID;
import static id.global.common.constants.iris.MessagingHeaders.Message.JWT;
import static id.global.common.constants.iris.MessagingHeaders.Message.ORIGIN_SERVICE_ID;
import static id.global.common.constants.iris.MessagingHeaders.Message.ROUTER;
import static id.global.common.constants.iris.MessagingHeaders.Message.SERVER_TIMESTAMP;
import static id.global.common.constants.iris.MessagingHeaders.Message.SESSION_ID;
import static id.global.common.constants.iris.MessagingHeaders.Message.USER_ID;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
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

import id.global.common.annotations.iris.ExchangeType;
import id.global.common.annotations.iris.Scope;
import id.global.iris.amqp.parsers.ExchangeParser;
import id.global.iris.amqp.parsers.ExchangeTypeParser;
import id.global.iris.amqp.parsers.MessageScopeParser;
import id.global.iris.amqp.parsers.RoutingKeyParser;
import id.global.iris.messaging.runtime.EventAppInfoProvider;
import id.global.iris.messaging.runtime.InstanceInfoProvider;
import id.global.iris.messaging.runtime.TimestampProvider;
import id.global.iris.messaging.runtime.api.message.ResourceUpdate;
import id.global.iris.messaging.runtime.channel.ChannelKey;
import id.global.iris.messaging.runtime.channel.ChannelService;
import id.global.iris.messaging.runtime.configuration.AmqpConfiguration;
import id.global.iris.messaging.runtime.context.EventAppContext;
import id.global.iris.messaging.runtime.context.EventContext;
import id.global.iris.messaging.runtime.exception.AmqpSendException;
import id.global.iris.messaging.runtime.exception.AmqpTransactionException;
import id.global.iris.messaging.runtime.tx.TransactionCallback;

@ApplicationScoped
public class AmqpProducer {
    private static final Logger log = LoggerFactory.getLogger(AmqpProducer.class);

    public static final String SERVICE_ID_UNAVAILABLE_FALLBACK = "N/A";
    private static final long WAIT_TIMEOUT_MILLIS = 2000;
    public static final EnumSet<Scope> CLIENT_MESSAGE_SCOPES = EnumSet.of(Scope.USER, Scope.SESSION, Scope.BROADCAST);

    private final ChannelService channelService;
    private final ObjectMapper objectMapper;
    private final EventContext eventContext;
    private final AmqpConfiguration configuration;
    private final TransactionManager transactionManager;
    private final CorrelationIdProvider correlationIdProvider;
    private final InstanceInfoProvider instanceInfoProvider;
    private final EventAppInfoProvider eventAppInfoProvider;
    private final TimestampProvider timestampProvider;

    private final AtomicInteger count = new AtomicInteger(0);
    private final Object lock = new Object();

    private final Map<Transaction, List<Message>> transactionDelayedMessages = new ConcurrentHashMap<>();

    private TransactionCallback transactionCallback;

    @Inject
    public AmqpProducer(@Named("producerChannelService") ChannelService channelService, ObjectMapper objectMapper,
            EventContext eventContext,
            AmqpConfiguration configuration, TransactionManager transactionManager,
            CorrelationIdProvider correlationIdProvider, InstanceInfoProvider instanceInfoProvider,
            EventAppInfoProvider eventAppInfoProvider, TimestampProvider timestampProvider) {
        this.channelService = channelService;
        this.objectMapper = objectMapper;
        this.eventContext = eventContext;
        this.configuration = configuration;
        this.transactionManager = transactionManager;
        this.correlationIdProvider = correlationIdProvider;
        this.instanceInfoProvider = instanceInfoProvider;
        this.eventAppInfoProvider = eventAppInfoProvider;
        this.timestampProvider = timestampProvider;
    }

    /**
     * Send message using Iris infrastructure.
     *
     * @param message message
     * @throws AmqpSendException when sending fails
     * @throws AmqpTransactionException when sending fails within transactional context
     */
    public void send(final Object message) throws AmqpSendException, AmqpTransactionException {
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
     * @throws AmqpSendException when sending fails
     * @throws AmqpTransactionException when sending fails within transactional context
     */
    public void send(final Object message, final String userId) throws AmqpSendException, AmqpTransactionException {
        doSend(message, userId);
    }

    /**
     * Send message to Iris subscription service.
     *
     * @param message message
     * @param resourceType resource type
     * @param resourceId resource id
     * @throws AmqpSendException when sending fails
     * @throws AmqpTransactionException when sending fails within transactional context
     */
    public void sendToSubscription(final Object message, final String resourceType, final String resourceId)
            throws AmqpSendException, AmqpTransactionException {
        if (resourceType == null || resourceType.isBlank()) {
            throw new AmqpSendException("Resource type is required for subscription event!");
        }

        final var resourceUpdate = new ResourceUpdate(resourceType, resourceId, message);
        final var exchange = getMessageAnnotation(resourceUpdate).name();
        final var routingKey = String.format("%s.%s", resourceType, exchange);

        publish(resourceUpdate,
                new RoutingDetails(resourceType, exchange, ExchangeType.TOPIC, routingKey, null, null));
    }

    private void doSend(final Object message, final String userId) throws AmqpSendException {
        final var messageAnnotation = getMessageAnnotation(message);

        final var scope = MessageScopeParser.getFromAnnotationClass(messageAnnotation);

        switch (scope) {
            case INTERNAL -> publish(message, getRoutingDetailsFromAnnotation(messageAnnotation, scope, userId));
            case USER, SESSION, BROADCAST -> publish(message,
                    getRoutingDetailsForClientScope(messageAnnotation, scope, userId));
            default -> throw new AmqpSendException("Message scope " + scope + " not supported!");
        }
    }

    private RoutingDetails getRoutingDetailsFromAnnotation(final id.global.common.annotations.iris.Message messageAnnotation,
            final Scope scope, final String userId) {

        final var exchangeType = ExchangeTypeParser.getFromAnnotationClass(messageAnnotation);
        final var eventName = ExchangeParser.getFromAnnotationClass(messageAnnotation);
        final var routingKey = getRoutingKey(messageAnnotation, exchangeType);

        return new RoutingDetails(eventName, eventName, exchangeType, routingKey, scope, userId);
    }

    private RoutingDetails getRoutingDetailsForClientScope(final id.global.common.annotations.iris.Message messageAnnotation,
            final Scope scope, final String userId) {

        final var exchange = Optional.ofNullable(userId)
                .map(u -> USER.getValue())
                .orElseGet(() -> switch (scope) {
                case USER -> USER.getValue();
                case SESSION -> SESSION.getValue();
                case BROADCAST -> BROADCAST.getValue();
                default -> throw new AmqpSendException("Message scope " + scope + " not supported!");
                });

        final var eventName = ExchangeParser.getFromAnnotationClass(messageAnnotation);
        final var routingKey = String.format("%s.%s", eventName, exchange);

        return new RoutingDetails(eventName, exchange, ExchangeType.TOPIC, routingKey, scope, userId);
    }

    private id.global.common.annotations.iris.Message getMessageAnnotation(final Object message) {
        if (message == null) {
            throw new AmqpSendException("Null message can not be published!");
        }

        return Optional
                .ofNullable(message.getClass().getAnnotation(id.global.common.annotations.iris.Message.class))
                .orElseThrow(() -> new AmqpSendException("Message annotation is required."));
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
            throws AmqpSendException {

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
        final var properties = getOrCreateAmqpBasicProperties(routingDetails);
        transactionDelayedMessages.computeIfAbsent(tx, k -> new LinkedList<>())
                .add(new Message(message, routingDetails, properties, eventContext.getEnvelope()));
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

    private void executeTxPublish(Transaction transaction) throws IOException, AmqpSendException {
        LinkedList<Message> messageList = (LinkedList<Message>) transactionDelayedMessages.get(transaction);
        Message message = messageList.poll();

        while (message != null) {
            eventContext.setMessageContext(message.properties(), message.envelope());
            executePublish(message.message(), message.routingDetails());
            message = messageList.poll();
        }
    }

    private void executePublish(Object message, RoutingDetails routingDetails) throws AmqpSendException {
        final var exchange = routingDetails.exchange();
        final var routingKey = routingDetails.routingKey();

        try {
            final byte[] bytes = objectMapper.writeValueAsBytes(message);
            synchronized (this.lock) {
                final var properties = getOrCreateAmqpBasicProperties(routingDetails);
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
            throw new AmqpSendException("Exception executing publish.", e);
        }
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

    private AMQP.BasicProperties getOrCreateAmqpBasicProperties(final RoutingDetails routingDetails) {
        final var eventAppContext = Optional.ofNullable(eventAppInfoProvider.getEventAppContext());
        final var serviceId = eventAppContext.map(EventAppContext::getId).orElse(SERVICE_ID_UNAVAILABLE_FALLBACK);
        final var basicProperties = Optional.ofNullable(eventContext.getAmqpBasicProperties())
                .orElse(createAmqpBasicProperties(serviceId));

        return buildAmqpBasicPropertiesWithCustomHeaders(basicProperties, serviceId, routingDetails);
    }

    private AMQP.BasicProperties buildAmqpBasicPropertiesWithCustomHeaders(final AMQP.BasicProperties basicProperties,
            final String serviceId, final RoutingDetails routingDetails) {

        final var eventName = routingDetails.eventName();
        final var scope = routingDetails.scope();
        final var userId = routingDetails.userId();

        final var hostName = instanceInfoProvider.getInstanceName();
        final var headers = new HashMap<>(basicProperties.getHeaders());
        headers.put(CURRENT_SERVICE_ID, serviceId);
        headers.put(INSTANCE_ID, hostName);
        headers.put(EVENT_TYPE, eventName);
        headers.put(SERVER_TIMESTAMP, timestampProvider.getCurrentTimestamp());
        if (scope != Scope.INTERNAL) {
            // never propagate JWT when "leaving" backend
            headers.remove(JWT);
        }

        final var builder = basicProperties.builder();
        if (userId != null) {
            // when overriding user header, make sure, to clean possible existing event context properties
            builder.correlationId(correlationIdProvider.getCorrelationId());
            headers.put(ORIGIN_SERVICE_ID, serviceId);
            headers.remove(ROUTER);
            headers.remove(SESSION_ID);
            headers.put(USER_ID, userId);
        }

        return builder.headers(headers).build();
    }

    private AMQP.BasicProperties createAmqpBasicProperties(final String serviceId) {
        return new AMQP.BasicProperties().builder()
                .correlationId(correlationIdProvider.getCorrelationId())
                .headers(Map.of(ORIGIN_SERVICE_ID, serviceId))
                .build();
    }

    private String getRoutingKey(id.global.common.annotations.iris.Message messageAnnotation,
            final ExchangeType exchangeType) {
        if (exchangeType == ExchangeType.FANOUT) {
            return "";
        }

        return RoutingKeyParser.getFromAnnotationClass(messageAnnotation);
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
                executeTxPublish(tx);

                if (isCallbackPresent) {
                    transactionCallback.afterTxPublish();
                }
            } catch (IOException | AmqpSendException e) {
                log.error("Exception completing send transaction.", e);
                throw new AmqpTransactionException("Exception completing send transaction");
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
