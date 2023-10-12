package org.iris_events.consumer;

import java.lang.invoke.MethodHandle;
import java.security.Principal;
import java.util.Optional;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import org.iris_events.auth.IrisJwtValidator;
import org.iris_events.common.MDCEnricher;
import org.iris_events.common.MDCProperties;
import org.iris_events.context.EventContext;
import org.iris_events.context.IrisContext;
import org.iris_events.context.MethodHandleContext;
import org.iris_events.producer.EventProducer;
import org.iris_events.runtime.IrisExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import io.quarkus.arc.Arc;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.reactive.messaging.providers.helpers.VertxContext;

public class DeliverCallbackProvider {
    private final EventContext eventContext;
    private final ObjectMapper objectMapper;
    private final EventProducer producer;
    private final IrisContext irisContext;
    private final Object eventHandlerInstance;
    private final MethodHandle methodHandle;
    private final MethodHandleContext methodHandleContext;
    private final IrisJwtValidator jwtValidator;
    private final IrisExceptionHandler errorHandler;

    private final static Logger log = LoggerFactory.getLogger(DeliverCallbackProvider.class);

    public DeliverCallbackProvider(
            final ObjectMapper objectMapper,
            final EventProducer producer,
            final IrisContext irisContext,
            final EventContext eventContext,
            final Object eventHandlerInstance,
            final MethodHandle methodHandle,
            final MethodHandleContext methodHandleContext,
            final IrisJwtValidator jwtValidator,
            final IrisExceptionHandler errorHandler) {

        this.objectMapper = objectMapper;
        this.producer = producer;
        this.irisContext = irisContext;
        this.eventHandlerInstance = eventHandlerInstance;
        this.methodHandle = methodHandle;
        this.methodHandleContext = methodHandleContext;
        this.jwtValidator = jwtValidator;
        this.eventContext = eventContext;
        this.errorHandler = errorHandler;
    }

    public DeliverCallback createDeliverCallback(final Channel channel) {
        return (consumerTag, message) -> {
            final var newDuplicatedContext = VertxContext.createNewDuplicatedContext();
            VertxContext.runOnContext(newDuplicatedContext, () -> handleMessage(channel, message));
        };
    }

    private void handleMessage(final Channel channel, final Delivery message) {
        try {
            Arc.container().requestContext().activate();
            final var properties = message.getProperties();
            final var envelope = message.getEnvelope();
            eventContext.setBasicProperties(properties);
            eventContext.setEnvelope(envelope);
            MDCEnricher.enrichMDC(properties);

            authorizeMessage();
            //todo we only do security association aka jwt token mapping to identity in iris.
            //todo we would need to better handle case where handler method requires auth / or specific roles

            final var handlerClassInstance = methodHandleContext.getHandlerClass().cast(eventHandlerInstance);
            final var messageObject = objectMapper.readValue(message.getBody(), methodHandleContext.getEventClass());
            final var invocationResult = methodHandle.invoke(handlerClassInstance, messageObject);
            final var optionalReturnEventClass = Optional.ofNullable(methodHandleContext.getReturnEventClass());
            optionalReturnEventClass.ifPresent(returnEventClass -> forwardMessage(invocationResult, returnEventClass));
            channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
        } catch (Throwable throwable) {
            log.error("Exception handling message", throwable);
            errorHandler.handleException(irisContext, message, channel, throwable);
        }
    }

    public IrisContext getIrisContext() {
        return irisContext;
    }

    private void authorizeMessage() {
        try {
            final SecurityIdentity securityIdentity = jwtValidator.authenticate(this.irisContext.getHandlerRolesAllowed());
            final Instance<CurrentIdentityAssociation> association = CDI.current().select(CurrentIdentityAssociation.class);
            if (!association.isResolvable()) {
                throw new AuthenticationFailedException("JWT identity association not resolvable.");
            }
            Optional.ofNullable(securityIdentity)
                    .map(SecurityIdentity::getPrincipal)
                    .map(Principal::getName)
                    .ifPresent(subject -> MDC.put(MDCProperties.GID_UUID, subject));
            association.get().setIdentity(securityIdentity);
        } catch (java.lang.SecurityException securityException) {
            throw IrisExceptionHandler.getSecurityException(securityException);
        }
    }

    private void forwardMessage(final Object invocationResult, final Class<?> returnEventClass) {
        final var returnClassInstance = returnEventClass.cast(invocationResult);
        producer.send(returnClassInstance);
    }
}
