package id.global.event.messaging.runtime.producer;

import static id.global.common.headers.amqp.MessagingHeaders.Message.CURRENT_SERVICE_ID;
import static id.global.common.headers.amqp.MessagingHeaders.Message.EVENT_TYPE;
import static id.global.common.headers.amqp.MessagingHeaders.Message.INSTANCE_ID;
import static id.global.common.headers.amqp.MessagingHeaders.Message.JWT;
import static id.global.common.headers.amqp.MessagingHeaders.Message.ORIGIN_SERVICE_ID;
import static id.global.common.headers.amqp.MessagingHeaders.Message.ROUTER;
import static id.global.common.headers.amqp.MessagingHeaders.Message.SESSION_ID;
import static id.global.common.headers.amqp.MessagingHeaders.Message.USER_ID;

import java.io.IOException;
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

import id.global.amqp.parsers.ExchangeParser;
import id.global.amqp.parsers.ExchangeTypeParser;
import id.global.amqp.parsers.MessageScopeParser;
import id.global.amqp.parsers.RoutingKeyParser;
import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.Scope;
import id.global.event.messaging.runtime.EventAppInfoProvider;
import id.global.event.messaging.runtime.InstanceInfoProvider;
import id.global.event.messaging.runtime.channel.ChannelKey;
import id.global.event.messaging.runtime.channel.ChannelService;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.context.EventAppContext;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.exception.AmqpSendException;
import id.global.event.messaging.runtime.exception.AmqpTransactionException;
import id.global.event.messaging.runtime.tx.TransactionCallback;

@ApplicationScoped
public class AmqpProducer {
    private static final Logger log = LoggerFactory.getLogger(AmqpProducer.class);

    public static final String SERVICE_ID_UNAVAILABLE_FALLBACK = "N/A";
    private static final long WAIT_TIMEOUT_MILLIS = 2000;

    private final ChannelService channelService;
    private final ObjectMapper objectMapper;
    private final EventContext eventContext;
    private final AmqpConfiguration configuration;
    private final TransactionManager transactionManager;
    private final CorrelationIdProvider correlationIdProvider;
    private final InstanceInfoProvider instanceInfoProvider;
    private final EventAppInfoProvider eventAppInfoProvider;

    private final AtomicInteger count = new AtomicInteger(0);
    private final Object lock = new Object();

    private final Map<Transaction, List<Message>> transactionDelayedMessages = new ConcurrentHashMap<>();

    private TransactionCallback transactionCallback;

    @Inject
    public AmqpProducer(@Named("producerChannelService") ChannelService channelService, ObjectMapper objectMapper,
            EventContext eventContext,
            AmqpConfiguration configuration, TransactionManager transactionManager,
            CorrelationIdProvider correlationIdProvider, InstanceInfoProvider instanceInfoProvider,
            EventAppInfoProvider eventAppInfoProvider) {
        this.channelService = channelService;
        this.objectMapper = objectMapper;
        this.eventContext = eventContext;
        this.configuration = configuration;
        this.transactionManager = transactionManager;
        this.correlationIdProvider = correlationIdProvider;
        this.instanceInfoProvider = instanceInfoProvider;
        this.eventAppInfoProvider = eventAppInfoProvider;
    }

    public void send(final Object message) throws AmqpSendException, AmqpTransactionException {
        doSend(message, null);
    }

    /**
     * Send message and override userId header of the message.
     * All following events caused by this event will have that user id set and any USER scoped messages will be sent to that
     * user.
     *
     * @param message message
     * @param userId user id
     */
    public void send(final Object message, final String userId) throws AmqpSendException, AmqpTransactionException {
        doSend(message, userId);
    }

    private void doSend(final Object message, final String userId) throws AmqpSendException, AmqpTransactionException {
        if (message == null) {
            throw new AmqpSendException("Null message can not be published!");
        }

        final id.global.common.annotations.amqp.Message messageAnnotation = Optional
                .ofNullable(message.getClass().getAnnotation(id.global.common.annotations.amqp.Message.class))
                .orElseThrow(() -> new AmqpSendException("Message annotation is required."));

        final var scope = MessageScopeParser.getFromAnnotationClass(messageAnnotation);
        final var exchangeType = ExchangeTypeParser.getFromAnnotationClass(messageAnnotation);
        final var exchange = ExchangeParser.getFromAnnotationClass(messageAnnotation);
        final var routingKey = getRoutingKey(messageAnnotation, exchangeType);
        final var amqpBasicProperties = getOrCreateAmqpBasicProperties(exchange, scope, userId);

        switch (scope) {
            case INTERNAL -> publish(message, exchange, routingKey, amqpBasicProperties, exchangeType);
            case USER -> publish(message, "user", "user", amqpBasicProperties, ExchangeType.TOPIC);
            case SESSION -> publish(message, "session", "session", amqpBasicProperties,
                    ExchangeType.TOPIC);
            case BROADCAST -> publish(message, "broadcast", "broadcast", amqpBasicProperties,
                    ExchangeType.TOPIC);
            default -> throw new AmqpSendException("Message scope " + scope + " not supported!");
        }
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
        transactionDelayedMessages.computeIfAbsent(tx, k -> new LinkedList<>())
                .add(new Message(message, exchange, routingKey, properties));
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
                String channelKey = ChannelKey.create(exchange, routingKey);
                Channel channel = channelService.getOrCreateChannelById(channelKey);
                log.info("publishing event to exchange: {}, routing key: {}, props: {}{", exchange, routingKey, properties);
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

    private AMQP.BasicProperties getOrCreateAmqpBasicProperties(String exchange,
            final Scope messageScope, final String userId) {
        final var eventAppContext = Optional.ofNullable(eventAppInfoProvider.getEventAppContext());
        final var serviceId = eventAppContext.map(EventAppContext::getId).orElse(SERVICE_ID_UNAVAILABLE_FALLBACK);
        final var basicProperties = Optional.ofNullable(eventContext.getAmqpBasicProperties())
                .orElse(createAmqpBasicProperties(serviceId));

        return buildAmqpBasicPropertiesWithCustomHeaders(basicProperties, serviceId, exchange, messageScope, userId);
    }

    private AMQP.BasicProperties buildAmqpBasicPropertiesWithCustomHeaders(final AMQP.BasicProperties basicProperties,
            final String serviceId, String exchange, final Scope messageScope, final String userId) {

        final var hostName = instanceInfoProvider.getInstanceName();
        final var headers = new HashMap<>(basicProperties.getHeaders());
        headers.put(CURRENT_SERVICE_ID, serviceId);
        headers.put(INSTANCE_ID, hostName);
        headers.put(EVENT_TYPE, exchange);
        if (messageScope != Scope.INTERNAL) {
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

    private String getRoutingKey(id.global.common.annotations.amqp.Message messageAnnotation,
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
                executePublish(transactionDelayedMessages.get(tx));

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
