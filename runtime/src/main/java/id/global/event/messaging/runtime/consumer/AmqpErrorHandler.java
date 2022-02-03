package id.global.event.messaging.runtime.consumer;

import static id.global.common.headers.amqp.MessagingHeaders.Message.EVENT_TYPE;
import static id.global.event.messaging.runtime.consumer.AmqpConsumer.ERROR_MESSAGE_EXCHANGE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

import id.global.common.headers.amqp.MessagingHeaders;
import id.global.event.messaging.runtime.api.error.MessagingError;
import id.global.event.messaging.runtime.api.error.SecurityError;
import id.global.event.messaging.runtime.api.error.ServerError;
import id.global.event.messaging.runtime.api.exception.BadMessageException;
import id.global.event.messaging.runtime.api.exception.MessagingException;
import id.global.event.messaging.runtime.api.exception.SecurityException;
import id.global.event.messaging.runtime.api.exception.ServerException;
import id.global.event.messaging.runtime.context.EventContext;
import id.global.event.messaging.runtime.error.ErrorMessage;
import id.global.event.messaging.runtime.exception.AmqpSendException;
import id.global.event.messaging.runtime.exception.AmqpTransactionException;
import id.global.event.messaging.runtime.requeue.MessageRequeueHandler;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;

@ApplicationScoped
public class AmqpErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(AmqpErrorHandler.class);
    public static final String ERROR_ROUTING_KEY_SUFFIX = ".error";

    private final ObjectMapper objectMapper;
    private final EventContext eventContext;
    private final MessageRequeueHandler retryEnqueuer;

    @Inject
    public AmqpErrorHandler(
            final ObjectMapper objectMapper,
            final EventContext eventContext,
            final MessageRequeueHandler retryEnqueuer) {

        this.objectMapper = objectMapper;
        this.eventContext = eventContext;
        this.retryEnqueuer = retryEnqueuer;
    }

    public void handleException(final Delivery message, final Channel channel, final Throwable throwable) {
        try {
            if (throwable instanceof AmqpSendException) {
                log.error("Exception sending/forwarding event.", throwable);
                throw new AmqpSendException("Exception sending/forwarding event.", throwable);
            } else if (throwable instanceof AmqpTransactionException) {
                log.error("Exception completing send transaction when sending/forwarding event.", throwable);
                throw new AmqpTransactionException("Exception completing send transaction when sending/forwarding event.",
                        throwable);
            } else if (throwable instanceof SecurityException) {
                handleSecurityException(message, channel, (SecurityException) throwable);
            } else if (throwable instanceof BadMessageException) {
                handleBadMessageException(message, channel, (BadMessageException) throwable);
            } else if (throwable instanceof ServerException) {
                handleServerException(message, channel, (ServerException) throwable);
            } else {
                handleServerException(ServerError.INTERNAL_SERVER_ERROR, message, channel, false, throwable);
            }
        } catch (IOException exception) {
            log.error("IOException encountered while handling error. Handled message will be requeued.", exception);
            throw new UncheckedIOException(exception);
        }
    }

    private void handleSecurityException(final Delivery message, final Channel channel, final SecurityException exception)
            throws IOException {
        final var originalExchange = message.getEnvelope().getExchange();
        final var originalRoutingKey = message.getEnvelope().getRoutingKey();
        log.error(String.format(
                "Authentication failed, message with given binding keys(s) is being discarded (acknowledged). error: '%s', exchange: '%s', routingKey: '%s'",
                exception.getCode(), originalExchange, originalRoutingKey), exception);

        acknowledgeMessage(message, channel, exception);
    }

    private void handleBadMessageException(final Delivery message, final Channel channel,
            final BadMessageException exception) throws IOException {
        final var originalExchange = message.getEnvelope().getExchange();
        final var originalRoutingKey = message.getEnvelope().getRoutingKey();
        log.error(String.format(
                "Bad message received, message with given binding keys(s) is being discarded (acknowledged). error: '%s', exchange: '%s', routingKey: '%s'",
                exception.getCode(), originalExchange, originalRoutingKey), exception);

        acknowledgeMessage(message, channel, exception);
    }

    private void handleServerException(final Delivery message, final Channel channel, final ServerException e)
            throws IOException {
        handleServerException(e.getMessagingError(), message, channel, e.shouldNotifyFrontend(), e);
    }

    private void handleServerException(final MessagingError messageError, final Delivery message, final Channel channel,
            boolean shouldNotifyFrontend, final Throwable throwable) throws IOException {
        log.error("Encountered server exception while processing message. Sending to retry exchange.", throwable);
        channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
        retryEnqueuer.enqueueWithBackoff(message, messageError.getClientCode(), shouldNotifyFrontend);
    }

    private void acknowledgeMessage(final Delivery message, final Channel channel, final MessagingException exception)
            throws IOException {

        final var errorMessage = new ErrorMessage(exception.getMessagingError(), exception.getMessage());
        sendErrorMessage(errorMessage, message, channel);

        channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
    }

    private void sendErrorMessage(ErrorMessage message, Delivery consumedMessage, Channel channel) {
        final var headers = new HashMap<>(eventContext.getHeaders());
        headers.remove(MessagingHeaders.Message.JWT);
        headers.put(EVENT_TYPE, ERROR_MESSAGE_EXCHANGE);
        final var basicProperties = consumedMessage.getProperties().builder()
                .headers(headers)
                .build();
        final var routingKey = consumedMessage.getEnvelope().getExchange() + ERROR_ROUTING_KEY_SUFFIX;
        try {
            channel.basicPublish(ERROR_MESSAGE_EXCHANGE, routingKey, basicProperties, objectMapper.writeValueAsBytes(message));
        } catch (IOException e) {
            log.error("Unable to write error message as bytes. Discarding error message. Message: {}", message);
        }
    }

    protected static MessagingError getSecurityMessageError(java.lang.SecurityException securityException) {
        // TODO: change with switch once available as non-preview
        MessagingError error;
        if (securityException instanceof AuthenticationFailedException) {
            error = SecurityError.AUTHORIZATION_FAILED;
        } else if (securityException instanceof ForbiddenException) {
            error = SecurityError.FORBIDDEN;
        } else if (securityException instanceof UnauthorizedException) {
            error = SecurityError.UNAUTHORIZED;
        } else {
            error = SecurityError.AUTHORIZATION_FAILED;
        }
        return error;
    }
}
