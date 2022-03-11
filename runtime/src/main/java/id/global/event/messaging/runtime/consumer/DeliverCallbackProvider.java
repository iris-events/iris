package id.global.event.messaging.runtime.consumer;

import static id.global.event.messaging.runtime.exception.AmqpExceptionHandler.getSecurityMessageError;

import java.lang.invoke.MethodHandle;
import java.security.Principal;
import java.util.Optional;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import org.slf4j.MDC;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import id.global.event.messaging.runtime.api.exception.SecurityException;
import id.global.event.messaging.runtime.auth.GidJwtValidator;
import id.global.event.messaging.runtime.context.AmqpContext;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.context.MethodHandleContext;
import id.global.event.messaging.runtime.exception.AmqpExceptionHandler;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.arc.Arc;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;

public class DeliverCallbackProvider {

    public static final String GID_UUID = "gidUuid";
    private final EventContext eventContext;
    private final ObjectMapper objectMapper;
    private final AmqpProducer producer;
    private final AmqpContext amqpContext;
    private final Object eventHandlerInstance;
    private final MethodHandle methodHandle;
    private final MethodHandleContext methodHandleContext;
    private final GidJwtValidator jwtValidator;
    private final AmqpExceptionHandler errorHandler;

    public DeliverCallbackProvider(
            final ObjectMapper objectMapper,
            final AmqpProducer producer,
            final AmqpContext amqpContext,
            final EventContext eventContext,
            final Object eventHandlerInstance,
            final MethodHandle methodHandle,
            final MethodHandleContext methodHandleContext,
            final GidJwtValidator jwtValidator,
            final AmqpExceptionHandler errorHandler) {

        this.objectMapper = objectMapper;
        this.producer = producer;
        this.amqpContext = amqpContext;
        this.eventHandlerInstance = eventHandlerInstance;
        this.methodHandle = methodHandle;
        this.methodHandleContext = methodHandleContext;
        this.jwtValidator = jwtValidator;
        this.eventContext = eventContext;
        this.errorHandler = errorHandler;
    }

    public DeliverCallback createDeliverCallback(final Channel channel) {
        return (consumerTag, message) -> {
            final var currentContextMap = MDC.getCopyOfContextMap();
            MDC.clear();
            try {
                Arc.container().requestContext().activate();
                final var properties = message.getProperties();
                final var envelope = message.getEnvelope();
                this.eventContext.setMessageContext(properties, envelope);

                authorizeMessage();
                final var handlerClassInstance = methodHandleContext.getHandlerClass().cast(eventHandlerInstance);
                final var messageObject = objectMapper.readValue(message.getBody(), methodHandleContext.getEventClass());
                final var invocationResult = methodHandle.invoke(handlerClassInstance, messageObject);
                final var optionalReturnEventClass = Optional.ofNullable(methodHandleContext.getReturnEventClass());
                optionalReturnEventClass.ifPresent(returnEventClass -> forwardMessage(invocationResult, returnEventClass));
                channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
            } catch (Throwable throwable) {
                errorHandler.handleException(amqpContext, message, channel, throwable);
            } finally {
                MDC.setContextMap(currentContextMap);
            }
        };
    }

    public AmqpContext getAmqpContext() {
        return amqpContext;
    }

    private void authorizeMessage() {
        try {
            final SecurityIdentity securityIdentity = jwtValidator.authenticate(this.amqpContext.getHandlerRolesAllowed());
            final Instance<CurrentIdentityAssociation> association = CDI.current().select(CurrentIdentityAssociation.class);
            if (!association.isResolvable()) {
                throw new AuthenticationFailedException("JWT identity association not resolvable.");
            }
            Optional.ofNullable(securityIdentity)
                    .map(SecurityIdentity::getPrincipal)
                    .map(Principal::getName)
                    .ifPresent(subject -> MDC.put(GID_UUID, subject));
            association.get().setIdentity(securityIdentity);
        } catch (java.lang.SecurityException securityException) {
            final var securityMessageError = getSecurityMessageError(securityException);
            throw new SecurityException(securityMessageError, securityException.getMessage());
        }
    }

    private void forwardMessage(final Object invocationResult, final Class<?> returnEventClass) {
        final var returnClassInstance = returnEventClass.cast(invocationResult);
        producer.send(returnClassInstance);
    }
}
