package id.global.event.messaging.runtime.consumer;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import id.global.event.messaging.runtime.channel.ConsumerChannelService;
import id.global.event.messaging.runtime.context.AmqpContext;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.context.MethodHandleContext;
import id.global.event.messaging.runtime.exception.AmqpConnectionException;
import id.global.event.messaging.runtime.producer.AmqpProducer;

@ApplicationScoped
public class AmqpConsumerContainer {
    private static final Logger LOG = LoggerFactory.getLogger(AmqpConsumerContainer.class);

    private final ObjectMapper objectMapper;
    private final EventContext eventContext;
    private final Map<String, AmqpConsumer> consumerMap;
    private final ConsumerChannelService consumerChannelService;
    private final AmqpProducer producer;

    @Inject
    public AmqpConsumerContainer(
            final ObjectMapper objectMapper,
            final EventContext eventContext,
            final ConsumerChannelService consumerChannelService,
            final AmqpProducer producer) {

        this.consumerChannelService = consumerChannelService;
        this.consumerMap = new HashMap<>();
        this.objectMapper = objectMapper;
        this.eventContext = eventContext;
        this.producer = producer;
    }

    public void initConsumers() {
        consumerMap.forEach((queueName, consumer) -> {
            try {
                consumer.initChannel();
            } catch (IOException e) {
                String msg = String.format("Could not initialize consumer for queue %s", queueName);
                LOG.error(msg, e);
                throw new AmqpConnectionException(msg, e);
            }
        });
    }

    public void addConsumer(MethodHandle methodHandle, MethodHandleContext methodHandleContext, AmqpContext amqpContext,
            Object eventHandlerInstance) {
        consumerMap.put(UUID.randomUUID().toString(), new AmqpConsumer(
                objectMapper,
                methodHandle,
                methodHandleContext,
                amqpContext,
                consumerChannelService,
                eventHandlerInstance,
                eventContext,
                producer));
    }
}
