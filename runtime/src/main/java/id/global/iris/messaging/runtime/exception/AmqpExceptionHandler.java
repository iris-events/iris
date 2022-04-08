package id.global.iris.messaging.runtime.exception;

import static id.global.common.headers.amqp.MessagingHeaders.Message.EVENT_TYPE;
import static id.global.common.headers.amqp.MessagingHeaders.Message.SERVER_TIMESTAMP;

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
import id.global.common.iris.Exchanges;
import id.global.iris.messaging.runtime.TimestampProvider;
import id.global.iris.messaging.runtime.api.error.MessagingError;
import id.global.iris.messaging.runtime.api.error.SecurityError;
import id.global.iris.messaging.runtime.api.error.ServerError;
import id.global.iris.messaging.runtime.api.exception.BadMessageException;
import id.global.iris.messaging.runtime.api.exception.MessagingException;
import id.global.iris.messaging.runtime.api.exception.SecurityException;
import id.global.iris.messaging.runtime.api.exception.ServerException;
import id.global.iris.messaging.runtime.context.AmqpContext;
import id.global.iris.messaging.runtime.context.EventContext;
import id.global.iris.messaging.runtime.error.ErrorMessage;
import id.global.iris.messaging.runtime.requeue.MessageRequeueHandler;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;

@ApplicationScoped
public class AmqpExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AmqpExceptionHandler.class);
    public static final String ERROR_ROUTING_KEY_SUFFIX = ".error";

    private final ObjectMapper objectMapper;
    private final EventContext eventContext;
    private final MessageRequeueHandler retryEnqueuer;
    private final TimestampProvider timestampProvider;

    @Inject
    public AmqpExceptionHandler(
            final ObjectMapper objectMapper,
            final EventContext eventContext,
            final MessageRequeueHandler retryEnqueuer,
            final TimestampProvider timestampProvider) {

        this.objectMapper = objectMapper;
        this.eventContext = eventContext;
        this.retryEnqueuer = retryEnqueuer;
        this.timestampProvider = timestampProvider;
    }

    public void handleException(final AmqpContext amqpContext, final Delivery message, final Channel channel,
            final Throwable throwable) {
        try {
            if (throwable instanceof AmqpTransactionException) {
                log.error("Exception completing send transaction when sending/forwarding event.", throwable);
                throw (AmqpTransactionException) throwable;
            } else if (throwable instanceof AmqpSendException) {
                log.error("Exception sending/forwarding event.", throwable);
                throw (AmqpSendException) throwable;
            } else if (throwable instanceof SecurityException) {
                handleSecurityException(message, channel, (SecurityException) throwable);
            } else if (throwable instanceof BadMessageException) {
                handleBadMessageException(message, channel, (BadMessageException) throwable);
            } else {
                handleServerException(amqpContext, message, channel, throwable);
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

        acknowledgeMessageAndSendError(message, channel, exception);
    }

    private void handleBadMessageException(final Delivery message, final Channel channel,
            final BadMessageException exception) throws IOException {
        final var originalExchange = message.getEnvelope().getExchange();
        final var originalRoutingKey = message.getEnvelope().getRoutingKey();
        log.error(String.format(
                "Bad message received, message with given binding keys(s) is being discarded (acknowledged). error: '%s', exchange: '%s', routingKey: '%s'",
                exception.getCode(), originalExchange, originalRoutingKey), exception);

        acknowledgeMessageAndSendError(message, channel, exception);
    }

    private void handleServerException(final AmqpContext amqpContext,
            final Delivery message,
            final Channel channel,
            final Throwable throwable)
            throws IOException {

        MessagingError messageError = ServerError.SERVER_ERROR;
        boolean shouldNotifyFrontend = false;
        if (throwable instanceof ServerException serverException) {
            messageError = serverException.getMessagingError();
            shouldNotifyFrontend = serverException.shouldNotifyFrontend();
        }

        log.error("Encountered server exception while processing message. Sending to retry exchange.", throwable);
        acknowledgeMessage(channel, message);
        retryEnqueuer.enqueueWithBackoff(amqpContext, message, messageError.getClientCode(), shouldNotifyFrontend);
    }

    private void acknowledgeMessageAndSendError(
            final Delivery message,
            final Channel channel,
            final MessagingException exception) throws IOException {

        final var errorMessage = new ErrorMessage(exception.getMessagingError(), exception.getMessage());
        sendErrorMessage(errorMessage, message, channel);

        acknowledgeMessage(channel, message);
    }

    private void acknowledgeMessage(final Channel channel, final Delivery message) throws IOException {
        channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
    }

    private void sendErrorMessage(ErrorMessage message, Delivery consumedMessage, Channel channel) {
        final var headers = new HashMap<>(eventContext.getHeaders());
        headers.remove(MessagingHeaders.Message.JWT);
        headers.put(EVENT_TYPE, Exchanges.ERROR.getValue());
        headers.put(SERVER_TIMESTAMP, timestampProvider.getCurrentTimestamp());
        final var basicProperties = consumedMessage.getProperties().builder()
                .headers(headers)
                .build();
        final var routingKey = consumedMessage.getEnvelope().getExchange() + ERROR_ROUTING_KEY_SUFFIX;
        try {
            channel.basicPublish(Exchanges.ERROR.getValue(), routingKey, basicProperties,
                    objectMapper.writeValueAsBytes(message));
        } catch (IOException e) {
            log.error("Unable to write error message as bytes. Discarding error message. Message: {}", message);
        }
    }

    public static SecurityError getSecurityMessageError(java.lang.SecurityException securityException) {
        // TODO: change with switch once available as non-preview
        SecurityError error;
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
