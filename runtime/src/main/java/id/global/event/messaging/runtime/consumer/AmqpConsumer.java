package id.global.event.messaging.runtime.consumer;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.Scope;
import id.global.event.messaging.runtime.HostnameProvider;
import id.global.event.messaging.runtime.channel.ConsumerChannelService;
import id.global.event.messaging.runtime.context.AmqpContext;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.context.MethodHandleContext;
import id.global.event.messaging.runtime.exception.AmqpRuntimeException;
import id.global.event.messaging.runtime.exception.AmqpSendException;
import id.global.event.messaging.runtime.exception.AmqpTransactionException;
import id.global.event.messaging.runtime.exception.AmqpTransactionRuntimeException;
import id.global.event.messaging.runtime.producer.AmqpProducer;

public class AmqpConsumer {
    private static final Logger log = LoggerFactory.getLogger(AmqpConsumer.class);
    private static final int FRONT_MESSAGE_TTL = 15000;
    private static final String DEAD_LETTER_FRONTEND = "dead-letter-frontend";
    private static final String DEAD_LETTER = "dead-letter";

    private final ObjectMapper objectMapper;
    private final MethodHandle methodHandle;
    private final MethodHandleContext methodHandleContext;
    private final AmqpContext context;
    private final ConsumerChannelService channelService;
    private final Object eventHandlerInstance;
    private final EventContext eventContext;
    private final AmqpProducer producer;
    private final HostnameProvider hostnameProvider;
    private final String channelId;
    private final DeliverCallback callback;
    private final String applicationName;

    public AmqpConsumer(
            final ObjectMapper objectMapper,
            final MethodHandle methodHandle,
            final MethodHandleContext methodHandleContext,
            final AmqpContext context,
            final ConsumerChannelService channelService,
            final Object eventHandlerInstance,
            final EventContext eventContext,
            final AmqpProducer producer,
            final HostnameProvider hostnameProvider,
            final String applicationName) {

        this.objectMapper = objectMapper;
        this.methodHandle = methodHandle;
        this.methodHandleContext = methodHandleContext;
        this.context = context;
        this.channelService = channelService;
        this.eventHandlerInstance = eventHandlerInstance;
        this.eventContext = eventContext;
        this.producer = producer;
        this.hostnameProvider = hostnameProvider;
        this.channelId = UUID.randomUUID().toString();
        this.callback = createDeliverCallback();
        this.applicationName = applicationName;
    }

    protected AmqpContext getContext() {
        return context;
    }

    private DeliverCallback createDeliverCallback() {
        return (consumerTag, message) -> {
            final var currentContextMap = MDC.getCopyOfContextMap();
            MDC.clear();
            try {
                this.eventContext.setAmqpBasicProperties(message.getProperties());

                final var handlerClassInstance = methodHandleContext.getHandlerClass().cast(eventHandlerInstance);
                final var messageObject = objectMapper.readValue(message.getBody(), methodHandleContext.getEventClass());

                final var invocationResult = methodHandle.invoke(handlerClassInstance, messageObject);

                final var optionalReturnEventClass = Optional.ofNullable(methodHandleContext.getReturnEventClass());
                optionalReturnEventClass.ifPresent(returnEventClass -> forwardMessage(invocationResult, returnEventClass));
            } catch (Throwable throwable) {
                log.error("Could not invoke method handler on for bindingKey {} queue: ",
                        Arrays.toString(this.context.getBindingKeys()), throwable);
            } finally {
                MDC.setContextMap(currentContextMap);
            }
        };
    }

    private void forwardMessage(final Object invocationResult, final Class<?> returnEventClass) {
        final var returnClassInstance = returnEventClass.cast(invocationResult);
        try {
            producer.send(returnClassInstance);
        } catch (AmqpSendException e) {
            log.error("Exception forwarding event.", e);
            throw new AmqpRuntimeException("Exception forwarding event.", e);
        } catch (AmqpTransactionException e) {
            log.error("Exception completing send transaction when sending forwarded event.", e);
            throw new AmqpTransactionRuntimeException("Exception completing send transaction when sending forwarded event.", e);
        }
    }

    public void initChannel() throws IOException {
        Channel channel = channelService.getOrCreateChannelById(this.channelId);
        if (this.context.getExchangeType() == ExchangeType.DIRECT) {
            declareDirect(channel);
        } else if (this.context.getExchangeType() == ExchangeType.TOPIC) {
            declareTopic(channel);
        } else {
            createQueues(channel);
        }
    }

    public DeliverCallback getCallback() {
        return callback;
    }

    private void declareDirect(Channel channel) throws IOException {
        // Normal consume
        AMQP.Queue.DeclareOk declareOk = channel.queueDeclare(this.context.getBindingKeys()[0], true, false,
                false, null);
        if (this.context.getName() != null && !this.context.getName().equals("")) {
            channel.exchangeDeclare(this.context.getName(), BuiltinExchangeType.DIRECT, true);
            channel.queueBind(declareOk.getQueue(), this.context.getName(), declareOk.getQueue());
        }

        channel.basicConsume(this.context.getBindingKeys()[0], true, this.callback, consumerTag -> {
        });
    }

    private void declareTopic(Channel channel) throws IOException {
        channel.exchangeDeclare(this.context.getName(), BuiltinExchangeType.TOPIC, true);
        AMQP.Queue.DeclareOk declareOk = channel.queueDeclare("", true, true, false, null);

        if (this.context.getBindingKeys() == null || this.context.getBindingKeys().length == 0) {
            throw new RuntimeException("Binding keys are required when declaring a TOPIC type exchange.");
        }

        for (String bindingKey : context.getBindingKeys()) {
            channel.queueBind(declareOk.getQueue(), context.getName(), bindingKey);
        }
        channel.basicConsume(declareOk.getQueue(), this.callback, consumerTag -> {
        });
    }

    private void declareFanout(Channel channel) throws IOException {
        channel.exchangeDeclare(this.context.getName(), BuiltinExchangeType.FANOUT, true);
        AMQP.Queue.DeclareOk declareOk = channel.queueDeclare("", true, true, false, null);
        channel.queueBind(declareOk.getQueue(), this.context.getName(), "");
        channel.basicConsume(declareOk.getQueue(), true, this.callback, consumerTag -> {
        });
    }

    private void createQueues(Channel channel) throws IOException {
        String exchange = context.getName();
        String versionQueue = "#";
        long ttl = context.getTtl();
        String deadLetter = context.getDeadLetterQueue();
        boolean durable = context.isDurable();
        boolean onEveryInstance = context.isConsumerOnEveryInstance();
        boolean autoDelete = context.isAutoDelete() && !onEveryInstance;
        String instanceName = hostnameProvider.getHostName();

        // prefetch count todo
        int prefetchCount = context.getPrefetch();
        channel.basicQos(prefetchCount);

        // if is FRONTEND then we set different default parameters
        if (context.getScope() == Scope.FRONTEND) {
            ttl = ttl == -1 ? FRONT_MESSAGE_TTL : ttl;
            deadLetter = deadLetter.trim().equals(DEAD_LETTER) ? DEAD_LETTER_FRONTEND : deadLetter;
            durable = false;
        }

        final String nameSuffix = getQueueName(exchange, instanceName, onEveryInstance);
        Map<String, Object> args = new HashMap<>();

        // setup dead letter
        if (!deadLetter.isBlank()) {
            String deadLetterQueue = "dead." + deadLetter;
            args.put("x-dead-letter-routing-key", getDeadPrefix(nameSuffix));
            args.put("x-dead-letter-exchange", deadLetter);
            channel.exchangeDeclare(deadLetter, BuiltinExchangeType.TOPIC);
            AMQP.Queue.DeclareOk declareOk = channel.queueDeclare(deadLetterQueue, true, false, false, null);
            channel.queueBind(deadLetterQueue, deadLetter, "#");
        }

        // time to leave of queue
        if (ttl >= 0) {
            args.put("x-message-ttl", ttl);
        }

        try {
            //amqpAdmin.declareQueue(new Queue(nameSuffix, durable, false, autoDelete, args));
            AMQP.Queue.DeclareOk declareOk = channel.queueDeclare(nameSuffix, durable, false, autoDelete, args);
            log.info("queue: {}, consumers: {}, message count: {}", declareOk.getQueue(), declareOk.getConsumerCount(),
                    declareOk.getMessageCount());
        } catch (IOException e) {
            long msgCount = channel.messageCount(nameSuffix);
            if (msgCount <= 0) {
                channel.queueDelete(nameSuffix, false, true);
                AMQP.Queue.DeclareOk declareOk = channel.queueDeclare(nameSuffix, durable, false, autoDelete, args);
            } else {
                log.error("The new settings of queue was not set, because was not empty! queue={}", nameSuffix, e);
            }
        }
        //context.getExchangeType() todo maybe use annotation info for setting exchange
        channel.exchangeDeclare(exchange, BuiltinExchangeType.FANOUT);
        //String routingKey = "#." + exchange + "." + versionQueue;
        String routingKey = "#." + exchange;
        channel.queueBind(nameSuffix, exchange, routingKey);
        channel.basicConsume(nameSuffix, true, this.callback, consumerTag -> {
            log.warn("Channel canceled for {}", nameSuffix);
        },
                (consumerTag, sig) -> {
                    log.warn("Channel shut down for with signal:{}, queue: {}, consumer: {}", sig, nameSuffix, consumerTag);
                });
        log.info("consumer started on '{}' --> {} routing key: {}", nameSuffix, exchange, routingKey);
    }

    private String getQueueName(String name, String instanceName, boolean onEveryInstance) {
        StringBuilder stringBuffer = new StringBuilder()
                .append(applicationName)
                .append(".")
                .append(name);

        if (onEveryInstance && instanceName != null && !instanceName.isBlank()) {
            stringBuffer.append(".").append(instanceName);
        }

        return stringBuffer.toString();
    }

    private String getDeadPrefix(String name) {
        return "dead." + name;
    }
}
