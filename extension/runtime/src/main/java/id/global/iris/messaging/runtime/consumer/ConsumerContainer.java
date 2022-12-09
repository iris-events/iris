package id.global.iris.messaging.runtime.consumer;

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

import id.global.iris.messaging.runtime.QueueNameProvider;
import id.global.iris.messaging.runtime.auth.GidJwtValidator;
import id.global.iris.messaging.runtime.channel.ChannelService;
import id.global.iris.messaging.runtime.context.EventContext;
import id.global.iris.messaging.runtime.context.IrisContext;
import id.global.iris.messaging.runtime.context.MethodHandleContext;
import id.global.iris.messaging.runtime.exception.IrisConnectionException;
import id.global.iris.messaging.runtime.exception.IrisExceptionHandler;
import id.global.iris.messaging.runtime.producer.EventProducer;
import id.global.iris.messaging.runtime.producer.ExchangeDeclarator;

@ApplicationScoped
public class ConsumerContainer {
    private static final Logger log = LoggerFactory.getLogger(ConsumerContainer.class);

    private final ObjectMapper objectMapper;
    private final EventContext eventContext;
    private final Map<String, Consumer> consumerMap;
    private final ChannelService consumerChannelService;
    private final EventProducer producer;
    private final QueueNameProvider queueNameProvider;
    private final GidJwtValidator jwtValidator;
    private final FrontendEventConsumer frontendEventConsumer;
    private final IrisExceptionHandler errorHandler;
    private final QueueDeclarator queueDeclarator;
    private final ExchangeDeclarator exchangeDeclarator;

    @Inject
    public ConsumerContainer(
            final ObjectMapper objectMapper,
            final EventContext eventContext,
            @Named("consumerChannelService") final ChannelService consumerChannelService,
            final EventProducer producer,
            final QueueNameProvider queueNameProvider,
            final GidJwtValidator jwtValidator,
            final FrontendEventConsumer frontendEventConsumer,
            final IrisExceptionHandler errorHandler,
            final QueueDeclarator queueDeclarator,
            final ExchangeDeclarator exchangeDeclarator) {

        this.consumerChannelService = consumerChannelService;
        this.queueNameProvider = queueNameProvider;
        this.jwtValidator = jwtValidator;
        this.errorHandler = errorHandler;
        this.queueDeclarator = queueDeclarator;
        this.exchangeDeclarator = exchangeDeclarator;
        this.consumerMap = new HashMap<>();
        this.objectMapper = objectMapper;
        this.eventContext = eventContext;
        this.producer = producer;
        this.frontendEventConsumer = frontendEventConsumer;
    }

    public void initConsumers() {
        consumerMap.forEach((queueName, consumer) -> {
            try {
                consumer.initChannel();
            } catch (Exception e) {
                String msg = String.format("Could not initialize consumer for exchange: '%s' queue '%s'",
                        consumer.getContext().getName(), queueName);
                log.error(msg, e);
                throw new IrisConnectionException(msg, e);
            }
        });
    }

    public void addConsumer(MethodHandle methodHandle, MethodHandleContext methodHandleContext, IrisContext irisContext,
            Object eventHandlerInstance) {

        final var deliverCallbackProvider = new DeliverCallbackProvider(objectMapper,
                producer,
                irisContext,
                eventContext,
                eventHandlerInstance,
                methodHandle,
                methodHandleContext,
                jwtValidator,
                errorHandler);

        consumerMap.put(UUID.randomUUID().toString(), new Consumer(
                irisContext,
                consumerChannelService,
                deliverCallbackProvider,
                queueNameProvider,
                queueDeclarator,
                exchangeDeclarator));
    }

    public void addFrontendCallback(MethodHandle methodHandle, MethodHandleContext methodHandleContext,
            IrisContext irisContext, Object eventHandlerInstance) {

        DeliverCallbackProvider deliverCallbackProvider = new DeliverCallbackProvider(
                objectMapper,
                producer,
                irisContext,
                eventContext,
                eventHandlerInstance,
                methodHandle,
                methodHandleContext,
                jwtValidator,
                errorHandler);

        frontendEventConsumer.addDeliverCallbackProvider(getFrontendRoutingKey(irisContext), deliverCallbackProvider);
    }

    private String getFrontendRoutingKey(IrisContext irisContext) {
        return Optional.ofNullable(irisContext.getBindingKeys()).map(strings -> strings.get(0)).orElse(irisContext.getName());
    }
}
