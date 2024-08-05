package org.iris_events.producer;

import static org.iris_events.common.Exchanges.SUBSCRIPTION;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.validation.constraints.NotNull;

import org.iris_events.annotations.CachedMessage;
import org.iris_events.annotations.ExchangeType;
import org.iris_events.asyncapi.parsers.CacheableTtlParser;
import org.iris_events.asyncapi.parsers.ExchangeParser;
import org.iris_events.asyncapi.parsers.MessageScopeParser;
import org.iris_events.asyncapi.parsers.PersistentParser;
import org.iris_events.asyncapi.parsers.RoutingKeyParser;
import org.iris_events.asyncapi.parsers.RpcResponseClassParser;
import org.iris_events.common.message.ResourceMessage;
import org.iris_events.context.EventContext;
import org.iris_events.exception.IrisSendException;
import org.iris_events.exception.IrisTransactionException;
import org.iris_events.routing.RoutingDetailsProvider;
import org.iris_events.runtime.AnnotationValueExtractor;
import org.iris_events.runtime.BasicPropertiesProvider;
import org.iris_events.runtime.ExchangeNameProvider;
import org.iris_events.runtime.QueueNameProvider;
import org.iris_events.runtime.RpcMappingProvider;
import org.iris_events.runtime.channel.ChannelKey;
import org.iris_events.runtime.channel.ChannelService;
import org.iris_events.runtime.configuration.IrisConfig;
import org.iris_events.tx.TransactionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.ReturnCallback;
import com.rabbitmq.client.ReturnListener;

@ApplicationScoped
public class EventProducer {
    private static final Logger log = LoggerFactory.getLogger(EventProducer.class);

    private static final long WAIT_TIMEOUT_MILLIS = 2000;
    private static final String RESOURCE = "resource";

    private final ChannelService channelService;
    private final ObjectMapper objectMapper;
    private final EventContext eventContext;
    private final IrisConfig config;
    private final TransactionManager transactionManager;
    private final BasicPropertiesProvider basicPropertiesProvider;
    private final QueueNameProvider queueNameProvider;
    private final ExchangeNameProvider exchangeNameProvider;
    private final RpcMappingProvider rpcMappingProvider;
    private final RoutingDetailsProvider routingDetailsProvider;

    private final AtomicInteger count = new AtomicInteger(0);
    private final Object lock = new Object();

    private final Map<Transaction, List<Message>> transactionDelayedMessages = new ConcurrentHashMap<>();

    private TransactionCallback transactionCallback;

    @Inject
    public EventProducer(@Named("producerChannelService") ChannelService channelService,
            ObjectMapper objectMapper,
            EventContext eventContext,
            IrisConfig config,
            TransactionManager transactionManager,
            BasicPropertiesProvider basicPropertiesProvider,
            ExchangeNameProvider exchangeNameProvider,
            QueueNameProvider queueNameProvider,
            RpcMappingProvider rpcMappingProvider,
            RoutingDetailsProvider routingDetailsProvider) {
        this.channelService = channelService;
        this.objectMapper = objectMapper;
        this.eventContext = eventContext;
        this.config = config;
        this.transactionManager = transactionManager;
        this.basicPropertiesProvider = basicPropertiesProvider;
        this.exchangeNameProvider = exchangeNameProvider;
        this.queueNameProvider = queueNameProvider;
        this.rpcMappingProvider = rpcMappingProvider;
        this.routingDetailsProvider = routingDetailsProvider;
    }

    /**
     * Send message using Iris infrastructure.
     *
     * @param message message
     * @throws IrisSendException when sending fails
     * @throws IrisTransactionException when sending fails within transactional context
     */
    public void send(final Object message) throws IrisSendException, IrisTransactionException {
        doSend(message, null, true);
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
        doSend(message, userId, true);
    }

    /**
     * Send message using Iris infrastructure and define if correlationId should propagate further or not
     * <p>
     *
     * @param message message
     * @param propagate if the correlationId chain should propagate or not
     * @throws IrisSendException when sending fails
     * @throws IrisTransactionException when sending fails within transactional context
     */
    public void send(final Object message, final boolean propagate) {
        doSend(message, null, propagate);
    }

    /**
     * Send message using Iris infrastructure and define if correlationId should propagate further or not and override userId
     * header of the message
     * <p>
     *
     * @param message message
     * @param userId user id
     * @param propagate if the correlationId chain should propagate or not
     * @throws IrisSendException when sending fails
     * @throws IrisTransactionException when sending fails within transactional context
     */
    public void send(final Object message, final String userId, final boolean propagate) {
        doSend(message, userId, propagate);
    }

    /**
     *
     */
    public <T> T sendAndReceive(final Object message, Class<T> responseType) {
        return sendAndReceive(message, responseType, config.getRpcTimeout());
    }

    /**
     *
     */
    public <T> T sendAndReceive(final Object message, Class<T> responseType, int timeout) {

        final var messageId = UUID.randomUUID();
        final var messageAnnotation = AnnotationValueExtractor.getMessageAnnotation(message);
        final var eventName = ExchangeParser.getFromAnnotationClass(messageAnnotation);

        final var replyToType = RpcResponseClassParser.getFromAnnotationClass(messageAnnotation).name();

        final var replyTo = rpcMappingProvider.getReplyTo(replyToType);
        if (replyTo == null) {
            throw new IrisSendException("Can not send RPC request message with missing replyTo parameter.");
        }

        try (Channel channel = channelService.createChannel()) {
            final var requestExchange = exchangeNameProvider.getRpcRequestExchangeName(eventName);
            final var responseExchange = exchangeNameProvider.getRpcResponseExchangeName(replyTo);
            final var requestQueueName = queueNameProvider.getRpcRequestQueueName(eventName);
            final var responseQueue = queueNameProvider.getRpcResponseQueueName(replyTo);

            channel.queueDeclare(responseQueue, false, false, true, null);
            channel.queueBind(responseQueue, responseExchange, responseQueue);

            final CompletableFuture<T> response = new CompletableFuture<>();
            String ctag = channel.basicConsume(responseQueue, false, (consumerTag, delivery) -> {
                final var messageObject = objectMapper.readValue(delivery.getBody(), responseType);
                final var answerMessageId = delivery.getProperties().getMessageId();

                if (messageId.toString().equals(answerMessageId)) {
                    response.complete(messageObject);
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } else {
                    channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                }
            }, consumerTag -> {
            });

            channel.basicPublish(
                    requestExchange,
                    requestQueueName,
                    new AMQP.BasicProperties.Builder()
                            .messageId(messageId.toString())
                            .replyTo(responseQueue)
                            .build(),
                    objectMapper.writeValueAsBytes(message));

            final var result = response.get(timeout, TimeUnit.MILLISECONDS);
            channel.basicCancel(ctag);
            return result;
        } catch (IOException | TimeoutException | InterruptedException | ExecutionException e) {
            throw new IrisSendException("Could not process rpc send", e);
        }
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
        final var messageAnnotation = AnnotationValueExtractor.getMessageAnnotation(message);

        if (resourceType == null || resourceType.isBlank()) {
            throw new IrisSendException("Resource type is required for subscription event!");
        }

        final var eventName = ExchangeParser.getFromAnnotationClass(messageAnnotation);
        final var routingKey = String.format("%s.%s", eventName, RESOURCE);
        final var resourceUpdate = new ResourceMessage(resourceType, resourceId, message);
        final var persistent = PersistentParser.getFromAnnotationClass(messageAnnotation);
        final var cacheTtl = getCachedAnnotation(message).map(CacheableTtlParser::getFromAnnotationClass).orElse(null);
        final var routingDetails = new RoutingDetails.Builder()
                .eventName(eventName)
                .exchange(SUBSCRIPTION.getValue())
                .exchangeType(ExchangeType.TOPIC)
                .routingKey(routingKey)
                .scope(null)
                .persistent(persistent)
                .cacheTtl(cacheTtl)
                .build();
        publish(resourceUpdate, routingDetails);
    }

    private void doSend(final Object message, final String userId, final boolean propagate) throws IrisSendException {
        log.debug("Sending Iris message: {}, userId: {}, propagate: {}", message, userId, propagate);
        final var messageAnnotation = AnnotationValueExtractor.getMessageAnnotation(message);

        final var scope = MessageScopeParser.getFromAnnotationClass(messageAnnotation);

        switch (scope) {
            case INTERNAL ->
                publish(message,
                        routingDetailsProvider.getRoutingDetailsFromAnnotation(messageAnnotation, scope, userId, propagate));
            case USER, SESSION, BROADCAST -> publish(message,
                    routingDetailsProvider.getRoutingDetailsForClientScope(messageAnnotation, scope, userId));
            default -> throw new IrisSendException("Message scope " + scope + " not supported!");
        }
    }

    // TODO make it not public
    //    public void sendRpcResponse(final Object message, final String replyTo) {
    //        log.info("Sending RPC response");
    //        final var messageAnnotation = AnnotationValueExtractor.getMessageAnnotation(message);
    //        executePublish(message, routingDetailsProvider.getRpcRoutingDetails(messageAnnotation, replyTo));
    //    }

    private Optional<CachedMessage> getCachedAnnotation(final Object message) {
        if (message == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(message.getClass().getAnnotation(CachedMessage.class));
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

    public void publish(@NotNull Object message, RoutingDetails routingDetails)
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
        Message message = messageList != null ? messageList.poll() : null;

        while (message != null) {
            final var envelope = message.envelope();
            final var properties = message.properties();
            final var routingDetails = message.routingDetails();

            eventContext.setEnvelope(envelope);
            eventContext.setBasicProperties(properties);
            executePublish(message.message(), routingDetails);
            message = messageList.poll();
        }
    }

    private void executePublish(Object message, RoutingDetails routingDetails) throws IrisSendException {
        final var exchange = routingDetails.getExchange();
        final var routingKey = routingDetails.getRoutingKey();

        try {
            final byte[] bytes = objectMapper.writeValueAsBytes(message);
            synchronized (this.lock) {
                final var properties = basicPropertiesProvider.getOrCreateAmqpBasicProperties(routingDetails);
                final var channelKey = ChannelKey.create(exchange, routingKey);
                final var channel = channelService.getOrCreateChannelById(channelKey);
                if (log.isTraceEnabled()) {
                    log.trace("publishing event to exchange: {}, routing key: {}, props: {}", exchange, routingKey, properties);
                }
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
            boolean isCallbackPresent = transactionCallback != null;
            if (isCallbackPresent) {
                transactionCallback.beforeCompletion(transactionDelayedMessages.get(tx));
            }
            // TODO: if publishing messages in "beforeCompletion" we introduce 2-PC race condition
        }

        @Override
        public void afterCompletion(int status) {
            boolean messagesPublished = false;
            try {
                if (status == Status.STATUS_COMMITTED) {
                    // TODO: if publishing messages in "afterCompletion" we are unable to rollback the transaction itself - synchronisation improvement required
                    executeTxPublish(tx);
                    messagesPublished = true;
                }
            } catch (IOException | IrisSendException e) {
                log.error("Exception completing send transaction.", e);
                throw new IrisTransactionException("Exception completing send transaction");
            } finally {
                if (transactionCallback != null) {
                    transactionCallback.afterCompletion(transactionDelayedMessages.get(tx), status, messagesPublished);
                }
                transactionDelayedMessages.remove(tx);
            }
        }
    }
}
