package id.global.event.messaging.runtime.consumer;

import static id.global.common.annotations.amqp.ExchangeType.FANOUT;
import static id.global.common.annotations.amqp.ExchangeType.TOPIC;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import id.global.event.messaging.runtime.channel.ConsumerChannelService;
import id.global.event.messaging.runtime.context.AmqpContext;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.context.MethodHandleContext;

public class AmqpConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(AmqpConsumer.class);

    private final String channelId;

    private final DeliverCallback callback;
    private final AmqpContext amqpContext;
    private final ConsumerChannelService channelService;
    private final EventContext eventContext;

    public AmqpConsumer(
            final MethodHandle methodHandle,
            final MethodHandleContext methodHandleContext,
            final AmqpContext amqpContext,
            final ConsumerChannelService channelService,
            final Object eventHandlerInstance,
            final ObjectMapper objectMapper,
            EventContext eventContext) {
        this.channelId = UUID.randomUUID().toString();
        this.eventContext = eventContext;
        this.channelService = channelService;
        this.amqpContext = amqpContext;
        this.callback = ((consumerTag, message) -> {
            try {
                Object cast = methodHandleContext.getHandlerClass().cast(eventHandlerInstance);
                this.eventContext.setAmqpBasicProperties(message.getProperties());
                methodHandle.invoke(cast, objectMapper.readValue(message.getBody(), methodHandleContext.getEventClass()));
            } catch (Throwable throwable) {
                LOG.error("Could not invoke method handler on queue: " + this.amqpContext.getQueue(), throwable);
            }
        });
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
        AMQP.Queue.DeclareOk declareOk = channel.queueDeclare(this.amqpContext.getQueue(), true, false,
                false, null);
        if (this.amqpContext.getExchange() != null && !this.amqpContext.getExchange().equals("")) {
            channel.exchangeDeclare(this.amqpContext.getExchange(), BuiltinExchangeType.DIRECT, true);
            channel.queueBind(declareOk.getQueue(), this.amqpContext.getExchange(), declareOk.getQueue());
        }

        channel.basicConsume(this.amqpContext.getQueue(), true, this.callback, consumerTag -> {
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
