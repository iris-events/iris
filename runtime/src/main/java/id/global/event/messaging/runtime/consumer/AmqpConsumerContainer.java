package id.global.event.messaging.runtime.consumer;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import id.global.event.messaging.runtime.InstanceInfoProvider;
import id.global.event.messaging.runtime.auth.GidJwtValidator;
import id.global.event.messaging.runtime.channel.ChannelService;
import id.global.event.messaging.runtime.context.AmqpContext;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.context.MethodHandleContext;
import id.global.event.messaging.runtime.exception.AmqpConnectionException;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import id.global.event.messaging.runtime.requeue.MessageRequeueHandler;

@ApplicationScoped
public class AmqpConsumerContainer {
    private static final Logger log = LoggerFactory.getLogger(AmqpConsumerContainer.class);

    private final ObjectMapper objectMapper;
    private final EventContext eventContext;
    private final Map<String, AmqpConsumer> consumerMap;
    private final ChannelService consumerChannelService;
    private final AmqpProducer producer;
    private final InstanceInfoProvider instanceInfoProvider;
    private final MessageRequeueHandler retryEnqueuer;
    private final GidJwtValidator jwtValidator;
    private final FrontendAmqpConsumer frontendAmqpConsumer;

    @Inject
    public AmqpConsumerContainer(
            final ObjectMapper objectMapper,
            final EventContext eventContext,
            @Named("consumerChannelService") final ChannelService consumerChannelService,
            final AmqpProducer producer,
            final InstanceInfoProvider instanceInfoProvider,
            final MessageRequeueHandler retryEnqueuer,
            final GidJwtValidator jwtValidator,
            final FrontendAmqpConsumer frontendAmqpConsumer) {

        this.consumerChannelService = consumerChannelService;
        this.instanceInfoProvider = instanceInfoProvider;
        this.jwtValidator = jwtValidator;
        this.consumerMap = new HashMap<>();
        this.objectMapper = objectMapper;
        this.eventContext = eventContext;
        this.producer = producer;
        this.retryEnqueuer = retryEnqueuer;
        this.frontendAmqpConsumer = frontendAmqpConsumer;
    }

    public void initConsumers() {
        consumerMap.forEach((queueName, consumer) -> {
            try {
                consumer.initChannel();
            } catch (Exception e) {
                String msg = String.format("Could not initialize consumer for exchange: '%s' queue '%s'",
                        consumer.getContext().getName(), queueName);
                log.error(msg, e);
                throw new AmqpConnectionException(msg, e);
            }
        });
    }

    public void addConsumer(MethodHandle methodHandle, MethodHandleContext methodHandleContext, AmqpContext amqpContext,
            Object eventHandlerInstance) {

        final var deliverCallbackProvider = new DeliverCallbackProvider(objectMapper,
                producer,
                amqpContext,
                eventContext,
                eventHandlerInstance,
                methodHandle,
                methodHandleContext,
                retryEnqueuer,
                jwtValidator);

        consumerMap.put(UUID.randomUUID().toString(), new AmqpConsumer(
                amqpContext,
                consumerChannelService,
                instanceInfoProvider,
                deliverCallbackProvider));
    }

    public void addFrontendCallback(MethodHandle methodHandle, MethodHandleContext methodHandleContext,
            AmqpContext amqpContext, Object eventHandlerInstance) {

        DeliverCallbackProvider deliverCallbackProvider = new DeliverCallbackProvider(
                objectMapper,
                producer,
                amqpContext,
                eventContext,
                eventHandlerInstance,
                methodHandle,
                methodHandleContext,
                retryEnqueuer,
                jwtValidator);

        frontendAmqpConsumer.addDeliverCallbackProvider(getFrontendRoutingKey(amqpContext), deliverCallbackProvider);
    }

    private String getFrontendRoutingKey(AmqpContext amqpContext) {
        return Optional.ofNullable(amqpContext.getBindingKeys()).map(strings -> strings.get(0)).orElse(amqpContext.getName());
    }
}
