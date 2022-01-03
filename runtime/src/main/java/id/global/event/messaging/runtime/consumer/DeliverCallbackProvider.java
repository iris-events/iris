package id.global.event.messaging.runtime.consumer;

import static id.global.event.messaging.runtime.consumer.AmqpConsumer.ERROR_MESSAGE_EXCHANGE;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Optional;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import id.global.common.headers.amqp.MessageHeaders;
import id.global.event.messaging.runtime.auth.GidJwtValidator;
import id.global.event.messaging.runtime.context.AmqpContext;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.context.MethodHandleContext;
import id.global.event.messaging.runtime.error.ErrorMessage;
import id.global.event.messaging.runtime.exception.AmqpRuntimeException;
import id.global.event.messaging.runtime.exception.AmqpSendException;
import id.global.event.messaging.runtime.exception.AmqpTransactionException;
import id.global.event.messaging.runtime.exception.AmqpTransactionRuntimeException;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import id.global.event.messaging.runtime.requeue.MessageRequeueHandler;
import id.global.event.messaging.runtime.requeue.RetryQueues;
import io.quarkus.arc.Arc;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;

public class DeliverCallbackProvider {

    private static final Logger log = LoggerFactory.getLogger(AmqpConsumer.class);

    public static final String AUTHORIZATION_FAILED_ERROR = "AUTHORIZATION_FAILED";
    public static final String UNAUTHORIZED_ERROR = "UNAUTHORIZED";
    public static final String FORBIDDEN_ERROR = "FORBIDDEN";
    public static final String MESSAGE_PROCESSING_ERROR = "MESSAGE_PROCESSING_ERROR";

    private final EventContext eventContext;
    private final ObjectMapper objectMapper;
    private final AmqpProducer producer;
    private final AmqpContext amqpContext;
    private final Object eventHandlerInstance;
    private final MethodHandle methodHandle;
    private final MethodHandleContext methodHandleContext;
    private final MessageRequeueHandler retryEnqueuer;
    private final RetryQueues retryQueues;
    private final GidJwtValidator jwtValidator;

    public DeliverCallbackProvider(
            final ObjectMapper objectMapper,
            final AmqpProducer producer,
            final AmqpContext amqpContext,
            final EventContext eventContext,
            final Object eventHandlerInstance,
            final MethodHandle methodHandle,
            final MethodHandleContext methodHandleContext,
            final MessageRequeueHandler retryEnqueuer,
            final RetryQueues retryQueues,
            final GidJwtValidator jwtValidator) {

        this.objectMapper = objectMapper;
        this.producer = producer;
        this.amqpContext = amqpContext;
        this.eventHandlerInstance = eventHandlerInstance;
        this.methodHandle = methodHandle;
        this.methodHandleContext = methodHandleContext;
        this.retryEnqueuer = retryEnqueuer;
        this.retryQueues = retryQueues;
        this.jwtValidator = jwtValidator;
        this.eventContext = eventContext;
    }

    public DeliverCallback createDeliverCallback(final Channel channel) {
        return (consumerTag, message) -> {
            final var currentContextMap = MDC.getCopyOfContextMap();
            MDC.clear();
            try {
                Arc.container().requestContext().activate();
                final var properties = message.getProperties();
                this.eventContext.setAmqpBasicProperties(properties);

                authorizeMessage();
                final var handlerClassInstance = methodHandleContext.getHandlerClass().cast(eventHandlerInstance);
                final var messageObject = objectMapper.readValue(message.getBody(), methodHandleContext.getEventClass());
                final var invocationResult = methodHandle.invoke(handlerClassInstance, messageObject);
                final var optionalReturnEventClass = Optional.ofNullable(methodHandleContext.getReturnEventClass());
                optionalReturnEventClass.ifPresent(returnEventClass -> forwardMessage(invocationResult, returnEventClass));
                channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
            } catch (SecurityException securityException) {
                handleSecurityException(message, channel, securityException);
            } catch (Throwable throwable) {
                handleMessageHandlingException(message, channel, throwable);
            } finally {
                MDC.setContextMap(currentContextMap);
            }
        };
    }

    private void handleMessageHandlingException(final Delivery message, final Channel channel, final Throwable throwable)
            throws IOException {

        final var retryCount = eventContext.getRetryCount();
        final var maxRetryCount = retryQueues.getMaxRetryCount();
        final var maxRetriesReached = retryCount >= maxRetryCount;

        final var bindingKeysString = getBindingKeysString();
        if (maxRetriesReached) {
            log.error(String.format(
                    "Could not invoke method handler and max retries (%d) are reached,"
                            + " message with given binding key(s) is being sent to DLQ. bindingKey(s): %s",
                    maxRetryCount, bindingKeysString), throwable);

            final var errorMessage = new ErrorMessage(MESSAGE_PROCESSING_ERROR, throwable.getMessage());
            sendErrorMessage(errorMessage, message, channel);
            channel.basicNack(message.getEnvelope().getDeliveryTag(), false, false);
        } else {
            log.error(String.format(
                    "Could not invoke method handler,"
                            + " message with given binding key(s) is being re-queued. bindingKey(s): %s, retry count: %s",
                    bindingKeysString, retryCount), throwable);
            channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
            retryEnqueuer.enqueueWithBackoff(message, retryCount);
        }
    }

    private void handleSecurityException(final Delivery message, final Channel channel,
            final SecurityException securityException) throws IOException {

        final var error = getSecurityExceptionError(securityException);
        final var bindingKeysString = getBindingKeysString();
        log.error(String.format(
                "Authentication failed, message with given binding keys(s) is being discarded (acknowledged). error: %s, bindingKey(s): %s",
                error, bindingKeysString), securityException);
        final var errorMessage = new ErrorMessage(error, securityException.getMessage());
        sendErrorMessage(errorMessage, message, channel);
        channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
    }

    private static String getSecurityExceptionError(SecurityException securityException) {
        // TODO: change with switch once available as non-preview
        String error;
        if (securityException instanceof AuthenticationFailedException) {
            error = AUTHORIZATION_FAILED_ERROR;
        } else if (securityException instanceof ForbiddenException) {
            error = FORBIDDEN_ERROR;
        } else if (securityException instanceof UnauthorizedException) {
            error = UNAUTHORIZED_ERROR;
        } else {
            error = AUTHORIZATION_FAILED_ERROR;
        }
        return error;
    }

    private String getBindingKeysString() {
        return Optional.ofNullable(this.amqpContext.getBindingKeys())
                .map(bindingKeys -> "[" + String.join(", ", bindingKeys) + "]")
                .orElse("[]");
    }

    private void authorizeMessage() {
        final SecurityIdentity securityIdentity = jwtValidator.authenticate(this.amqpContext.getHandlerRolesAllowed());
        final Instance<CurrentIdentityAssociation> association = CDI.current().select(CurrentIdentityAssociation.class);
        if (!association.isResolvable()) {
            throw new AuthenticationFailedException("JWT identity association not resolvable.");
        }
        association.get().setIdentity(securityIdentity);
    }

    private void sendErrorMessage(ErrorMessage message, Delivery consumedMessage, Channel channel) {
        final var headers = new HashMap<>(eventContext.getHeaders());
        headers.remove(MessageHeaders.JWT);
        final var basicProperties = consumedMessage.getProperties().builder()
                .headers(headers)
                .build();
        final var routingKey = consumedMessage.getEnvelope().getRoutingKey();
        try {
            channel.basicPublish(ERROR_MESSAGE_EXCHANGE, routingKey, basicProperties, objectMapper.writeValueAsBytes(message));
        } catch (IOException e) {
            log.error("Unable to write error message as bytes. Discarding error message. Message: {}", message);
        }
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
}
