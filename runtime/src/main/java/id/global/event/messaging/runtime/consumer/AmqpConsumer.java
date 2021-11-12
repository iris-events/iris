package id.global.event.messaging.runtime.consumer;

import static id.global.common.annotations.amqp.ExchangeType.FANOUT;
import static id.global.common.annotations.amqp.ExchangeType.TOPIC;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;
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

    private final ObjectMapper objectMapper;
    private final MethodHandle methodHandle;
    private final MethodHandleContext methodHandleContext;
    private final AmqpContext amqpContext;
    private final ConsumerChannelService channelService;
    private final Object eventHandlerInstance;
    private final EventContext eventContext;
    private final AmqpProducer producer;

    private final String channelId;
    private final DeliverCallback callback;

    public AmqpConsumer(
            final ObjectMapper objectMapper,
            final MethodHandle methodHandle,
            final MethodHandleContext methodHandleContext,
            final AmqpContext amqpContext,
            final ConsumerChannelService channelService,
            final Object eventHandlerInstance,
            final EventContext eventContext,
            final AmqpProducer producer) {

        this.objectMapper = objectMapper;
        this.methodHandle = methodHandle;
        this.methodHandleContext = methodHandleContext;
        this.amqpContext = amqpContext;
        this.channelService = channelService;
        this.eventHandlerInstance = eventHandlerInstance;
        this.eventContext = eventContext;
        this.producer = producer;

        this.channelId = UUID.randomUUID().toString();
        this.callback = createDeliverCallback();
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
                log.error("Could not invoke method handler on for bindingKeysqueue: " + Arrays.toString(
                        this.amqpContext.getBindingKeys()), throwable);
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

        if (this.amqpContext.getExchangeType().equals(FANOUT)) {
            declareFanout(channel);
        } else if (this.amqpContext.getExchangeType().equals(TOPIC)) {
            declareTopic(channel);
        } else {
            declareDirect(channel);
        }
    }

    public DeliverCallback getCallback() {
        return callback;
    }

    private void declareDirect(Channel channel) throws IOException {
        // Normal consume
        AMQP.Queue.DeclareOk declareOk = channel.queueDeclare(this.amqpContext.getBindingKeys()[0], true, false,
                false, null);
        if (this.amqpContext.getExchange() != null && !this.amqpContext.getExchange().equals("")) {
            channel.exchangeDeclare(this.amqpContext.getExchange(), BuiltinExchangeType.DIRECT, true);
            channel.queueBind(declareOk.getQueue(), this.amqpContext.getExchange(), declareOk.getQueue());
        }

        channel.basicConsume(this.amqpContext.getBindingKeys()[0], true, this.callback, consumerTag -> {
        });
    }

    private void declareTopic(Channel channel) throws IOException {
        channel.exchangeDeclare(this.amqpContext.getExchange(), BuiltinExchangeType.TOPIC, true);
        AMQP.Queue.DeclareOk declareOk = channel.queueDeclare("", true, true, false, null);

        if (this.amqpContext.getBindingKeys() == null || this.amqpContext.getBindingKeys().length == 0) {
            throw new RuntimeException("Binding keys are required when declaring a TOPIC type exchange.");
        }

        for (String bindingKey : amqpContext.getBindingKeys()) {
            channel.queueBind(declareOk.getQueue(), amqpContext.getExchange(), bindingKey);
        }
        channel.basicConsume(declareOk.getQueue(), this.callback, consumerTag -> {
        });
    }

    private void declareFanout(Channel channel) throws IOException {
        channel.exchangeDeclare(this.amqpContext.getExchange(), BuiltinExchangeType.FANOUT, true);
        AMQP.Queue.DeclareOk declareOk = channel.queueDeclare("", true, true, false, null);
        channel.queueBind(declareOk.getQueue(), this.amqpContext.getExchange(), "");
        channel.basicConsume(declareOk.getQueue(), true, this.callback, consumerTag -> {
        });
    }
}
