package org.iris_events.consumer;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.iris_events.auth.IrisJwtValidator;
import org.iris_events.context.EventContext;
import org.iris_events.context.IrisContext;
import org.iris_events.context.MethodHandleContext;
import org.iris_events.exception.IrisConnectionException;
import org.iris_events.producer.EventProducer;
import org.iris_events.routing.RoutingDetailsProvider;
import org.iris_events.runtime.ExchangeNameProvider;
import org.iris_events.runtime.InstanceInfoProvider;
import org.iris_events.runtime.IrisExceptionHandler;
import org.iris_events.runtime.QueueNameProvider;
import org.iris_events.runtime.channel.ChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class ConsumerContainer {
    private static final Logger log = LoggerFactory.getLogger(ConsumerContainer.class);

    private final ObjectMapper objectMapper;
    private final EventContext eventContext;
    private final Map<String, Consumer> consumerMap;
    private final ChannelService consumerChannelService;
    private final EventProducer producer;
    private final QueueNameProvider queueNameProvider;
    private final ExchangeNameProvider exchangeNameProvider;
    private final IrisJwtValidator jwtValidator;
    private final FrontendEventConsumer frontendEventConsumer;
    private final IrisExceptionHandler errorHandler;
    private final QueueDeclarator queueDeclarator;
    private final ExchangeDeclarator exchangeDeclarator;
    private final InstanceInfoProvider instanceInfoProvider;
    private final RoutingDetailsProvider routingDetailsProvider;

    @Inject
    public ConsumerContainer(
            final ObjectMapper objectMapper,
            final EventContext eventContext,
            @Named("consumerChannelService") final ChannelService consumerChannelService,
            final EventProducer producer,
            final QueueNameProvider queueNameProvider,
            final ExchangeNameProvider exchangeNameProvider,
            final Instance<IrisJwtValidator> jwtValidator,
            final FrontendEventConsumer frontendEventConsumer,
            final IrisExceptionHandler errorHandler,
            final QueueDeclarator queueDeclarator,
            final ExchangeDeclarator exchangeDeclarator,
            final InstanceInfoProvider instanceInfoProvider,
            final RoutingDetailsProvider routingDetailsProvider) {

        this.consumerChannelService = consumerChannelService;
        this.queueNameProvider = queueNameProvider;
        this.exchangeNameProvider = exchangeNameProvider;
        this.jwtValidator = jwtValidator.stream().findAny().orElse(null);
        this.errorHandler = errorHandler;
        this.queueDeclarator = queueDeclarator;
        this.exchangeDeclarator = exchangeDeclarator;
        this.consumerMap = new HashMap<>();
        this.objectMapper = objectMapper;
        this.eventContext = eventContext;
        this.producer = producer;
        this.frontendEventConsumer = frontendEventConsumer;
        this.instanceInfoProvider = instanceInfoProvider;
        this.routingDetailsProvider = routingDetailsProvider;
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
                errorHandler,
                routingDetailsProvider);

        consumerMap.put(UUID.randomUUID().toString(), new Consumer(
                irisContext,
                consumerChannelService,
                deliverCallbackProvider,
                queueNameProvider,
                exchangeNameProvider,
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
                errorHandler,
                routingDetailsProvider);

        frontendEventConsumer.addDeliverCallbackProvider(getFrontendRoutingKey(irisContext), deliverCallbackProvider);
    }

    private String getFrontendRoutingKey(IrisContext irisContext) {
        return Optional.ofNullable(irisContext.getBindingKeys()).map(strings -> strings.get(0)).orElse(irisContext.getName());
    }
}
