package id.global.event.messaging.runtime.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import id.global.event.messaging.runtime.context.AmqpContext;
import id.global.event.messaging.runtime.context.MethodHandleContext;
import id.global.event.messaging.runtime.enums.ExchangeType;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandle;

public class AmqpConsumer {
    private static final Logger LOG = Logger.getLogger(AmqpConsumer.class.getName());
    private final DeliverCallback callback;
    private final AmqpContext amqpContext;

    private Channel channel;

    public AmqpConsumer(
            final MethodHandle methodHandle,
            final MethodHandleContext methodHandleContext,
            final AmqpContext amqpContext,
            final Object eventHandlerInstance,
            final ObjectMapper objectMapper) {
        Object cast = methodHandleContext.getHandlerClass().cast(eventHandlerInstance);

        this.amqpContext = amqpContext;
        this.callback = ((consumerTag, message) -> {
            try {
                methodHandle.invoke(cast, objectMapper.readValue(message.getBody(), methodHandleContext.getEventClass()));
            } catch (Throwable throwable) {
                LOG.error("Could not invoke method handler on queue: " + this.amqpContext.getQueue(), throwable);
            }
        });
    }

    public void initChannel(Connection connection) throws IOException {
        this.channel = connection.createChannel();

        if (this.amqpContext.getExchangeType().equals(ExchangeType.FANOUT)) {
            // Fanout consume
            AMQP.Exchange.DeclareOk fanout = this.channel
                    .exchangeDeclare(this.amqpContext.getExchange(), BuiltinExchangeType.FANOUT);
            AMQP.Queue.DeclareOk declareOk = this.channel.queueDeclare("", false, true, false, null);
            AMQP.Queue.BindOk bindOk = channel.queueBind(declareOk.getQueue(), this.amqpContext.getExchange(), "");
            this.channel.basicConsume(declareOk.getQueue(), true, this.callback, consumerTag -> {
            });
        } else if (this.amqpContext.getExchangeType().equals(ExchangeType.TOPIC)) {
            AMQP.Exchange.DeclareOk topic = this.channel
                    .exchangeDeclare(this.amqpContext.getExchange(), BuiltinExchangeType.TOPIC);
            AMQP.Queue.DeclareOk declareOk = this.channel.queueDeclare("", false, true, false, null);

            if (this.amqpContext.getBindingKeys() == null || this.amqpContext.getBindingKeys().length == 0) {
                throw new RuntimeException("Binding keys are required when declaring a TOPIC type exchange.");
            }

            for (String bindingKey : amqpContext.getBindingKeys()) {
                channel.queueBind(declareOk.getQueue(), amqpContext.getExchange(), bindingKey);
            }
            channel.basicConsume(declareOk.getQueue(), this.callback, consumerTag -> {
            });
        } else {
            // Normal consume
            AMQP.Queue.DeclareOk declareOk = this.channel.queueDeclare(this.amqpContext.getQueue(), false, false, false, null);
            if (this.amqpContext.getExchange() != null && !this.amqpContext.getExchange().equals("")) {
                this.channel.exchangeDeclare(this.amqpContext.getExchange(), BuiltinExchangeType.DIRECT);
                this.channel.queueBind(declareOk.getQueue(), this.amqpContext.getExchange(), declareOk.getQueue());
            }

            this.channel.basicConsume(this.amqpContext.getQueue(), true, this.callback, consumerTag -> {
            });
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public DeliverCallback getCallback() {
        return callback;
    }
}
