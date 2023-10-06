package org.iris_events.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.SecurityException;
import java.util.HashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;

import org.iris_events.common.ErrorMessageDetailsBuilder;
import org.iris_events.common.ErrorType;
import org.iris_events.common.message.ErrorMessage;
import org.iris_events.context.EventContext;
import org.iris_events.context.IrisContext;
import org.iris_events.exception.*;
import org.iris_events.runtime.requeue.MessageRequeueHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;

@ApplicationScoped
public class IrisExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(IrisExceptionHandler.class);
    public static final String AUTHENTICATION_FAILED_CLIENT_CODE = "AUTHENTICATION_FAILED";
    public static final String FORBIDDEN_CLIENT_CODE = "FORBIDDEN";
    public static final String UNAUTHORIZED_CLIENT_CODE = "UNAUTHORIZED";
    public static final String SERVER_ERROR_CLIENT_CODE = "INTERNAL_SERVER_ERROR";

    private final ObjectMapper objectMapper;
    private final EventContext eventContext;
    private final MessageRequeueHandler retryEnqueuer;
    private final TimestampProvider timestampProvider;

    @Inject
    public IrisExceptionHandler(
            final ObjectMapper objectMapper,
            final EventContext eventContext,
            final MessageRequeueHandler retryEnqueuer,
            final TimestampProvider timestampProvider) {

        this.objectMapper = objectMapper;
        this.eventContext = eventContext;
        this.retryEnqueuer = retryEnqueuer;
        this.timestampProvider = timestampProvider;
    }

    public void handleException(final IrisContext irisContext, final Delivery message, final Channel channel,
            final Throwable throwable) {
        try {
            if (throwable instanceof IrisTransactionException) {
                log.error("Exception completing send transaction when sending/forwarding event.", throwable);
                throw (IrisTransactionException) throwable;
            } else if (throwable instanceof IrisSendException) {
                log.error("Exception sending/forwarding event.", throwable);
                throw (IrisSendException) throwable;
            } else if (throwable instanceof org.iris_events.exception.SecurityException) {
                handleSecurityException(message, channel, (org.iris_events.exception.SecurityException) throwable);
            } else if (throwable instanceof java.lang.SecurityException) {
                handleSecurityException(message, channel, (java.lang.SecurityException) throwable);
            } else if (throwable instanceof InvalidFormatException) {
                var clientException = new BadPayloadException(ErrorType.BAD_PAYLOAD.name(), throwable.getMessage());
                handleBadMessageException(message, channel, clientException);
            } else if (throwable instanceof ValidationException) {
                var clientException = new BadPayloadException(ErrorType.BAD_PAYLOAD.name(), throwable.getMessage());
                handleBadMessageException(message, channel, clientException);
            } else if (throwable instanceof ClientException) {
                handleBadMessageException(message, channel, (ClientException) throwable);
            } else {
                handleServerException(irisContext, message, channel, throwable);
            }
        } catch (IOException exception) {
            log.error("IOException encountered while handling error. Handled message will be requeued.", exception);
            throw new UncheckedIOException(exception);
        }
    }

    private void handleSecurityException(final Delivery message, final Channel channel,
            final SecurityException securityException) throws IOException {
        handleSecurityException(message, channel, getSecurityException(securityException));
    }

    private void handleSecurityException(final Delivery message, final Channel channel,
            final org.iris_events.exception.SecurityException exception)
            throws IOException {
        final var originalExchange = message.getEnvelope().getExchange();
        final var originalRoutingKey = message.getEnvelope().getRoutingKey();
        log.error(String.format(
                "Security exception thrown. Message with given binding keys(s) is being discarded (acknowledged). error: '%s', exchange: '%s', routingKey: '%s', errorType: '%s'",
                exception.getClientCode(), originalExchange, originalRoutingKey, exception.getClass().getName()), exception);

        acknowledgeMessageAndSendError(message, channel, exception);
    }

    private void handleBadMessageException(final Delivery message, final Channel channel,
            final ClientException exception) throws IOException {
        final var originalExchange = message.getEnvelope().getExchange();
        final var originalRoutingKey = message.getEnvelope().getRoutingKey();
        log.error(String.format(
                "Bad message received, message with given binding keys(s) is being discarded (acknowledged). error: '%s', exchange: '%s', routingKey: '%s'",
                exception.getClientCode(), originalExchange, originalRoutingKey), exception);

        acknowledgeMessageAndSendError(message, channel, exception);
    }

    private void handleServerException(final IrisContext irisContext,
            final Delivery message,
            final Channel channel,
            final Throwable throwable)
            throws IOException {

        ServerException exception;
        if (throwable instanceof ServerException serverException) {
            exception = serverException;
        } else {
            exception = new ServerException(SERVER_ERROR_CLIENT_CODE, throwable.getMessage(), false, throwable.getCause());
        }

        if (exception.shouldRetry()) {
            log.error("Encountered server exception while processing message. Sending to retry exchange.", throwable);
            acknowledgeMessage(channel, message);
            retryEnqueuer.enqueueWithBackoff(irisContext, message, exception, exception.shouldNotifyFrontend());
        } else if (exception.shouldNotifyFrontend()) {
            log.error("Encountered server exception while processing message. Notifying client with no retries.", throwable);
            acknowledgeMessageAndSendError(message, channel, exception);
        } else {
            log.error("Encountered server exception while processing message.", throwable);
            acknowledgeMessage(channel, message);
        }
    }

    private void acknowledgeMessageAndSendError(
            final Delivery message,
            final Channel channel,
            final MessagingException exception) throws IOException {

        final var errorMessage = new ErrorMessage(exception.getErrorType(), exception.getClientCode(), exception.getMessage());
        sendErrorMessage(errorMessage, message, channel);

        acknowledgeMessage(channel, message);
    }

    private void acknowledgeMessage(final Channel channel, final Delivery message) throws IOException {
        channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
    }

    private void sendErrorMessage(ErrorMessage message, Delivery consumedMessage, Channel channel) {
        final var originalExchange = consumedMessage.getEnvelope().getExchange();
        final var originalMessageHeaders = new HashMap<>(eventContext.getHeaders());
        final var currentTimestamp = timestampProvider.getCurrentTimestamp();
        final var errorMessageDetails = ErrorMessageDetailsBuilder
                .build(originalExchange,
                        originalMessageHeaders,
                        currentTimestamp);

        final var messageHeaders = errorMessageDetails.messageHeaders();
        final var basicProperties = consumedMessage.getProperties().builder()
                .headers(messageHeaders)
                .build();
        try {
            channel.basicPublish(
                    errorMessageDetails.exchange(),
                    errorMessageDetails.routingKey(),
                    basicProperties, objectMapper.writeValueAsBytes(message));
        } catch (IOException e) {
            log.error("Unable to write error message as bytes. Discarding error message. Message: {}", message);
        }
    }

    public static org.iris_events.exception.SecurityException getSecurityException(
            java.lang.SecurityException securityException) {
        final var message = securityException.getMessage();
        final var cause = securityException.getCause();

        // TODO: use switch with patterns once available
        if (securityException instanceof AuthenticationFailedException) {
            return new org.iris_events.exception.UnauthorizedException(AUTHENTICATION_FAILED_CLIENT_CODE, message, cause);
        } else if (securityException instanceof ForbiddenException) {
            return new org.iris_events.exception.ForbiddenException(FORBIDDEN_CLIENT_CODE, message, cause);
        } else if (securityException instanceof UnauthorizedException) {
            return new org.iris_events.exception.UnauthorizedException(UNAUTHORIZED_CLIENT_CODE, message, cause);
        }

        return new org.iris_events.exception.UnauthorizedException(UNAUTHORIZED_CLIENT_CODE, message, cause);
    }
}
