package id.global.iris.messaging.runtime.exception;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

import id.global.iris.common.error.ErrorMessageDetailsBuilder;
import id.global.iris.common.error.ErrorType;
import id.global.iris.common.exception.BadPayloadException;
import id.global.iris.common.exception.ClientException;
import id.global.iris.common.exception.MessagingException;
import id.global.iris.common.exception.SecurityException;
import id.global.iris.common.exception.ServerException;
import id.global.iris.common.message.ErrorMessage;
import id.global.iris.messaging.runtime.TimestampProvider;
import id.global.iris.messaging.runtime.context.EventContext;
import id.global.iris.messaging.runtime.context.IrisContext;
import id.global.iris.messaging.runtime.requeue.MessageRequeueHandler;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;

@ApplicationScoped
public class IrisExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(IrisExceptionHandler.class);
    public static final String ERROR_ROUTING_KEY_SUFFIX = ".error";
    public static final String AUTHENTICATION_FAILED_CLIENT_CODE = ErrorType.AUTHENTICATION_FAILED.name();
    public static final String FORBIDDEN_CLIENT_CODE = ErrorType.FORBIDDEN.name();
    public static final String UNAUTHORIZED_CLIENT_CODE = ErrorType.UNAUTHORIZED.name();
    public static final String SERVER_ERROR_CLIENT_CODE = ErrorType.INTERNAL_SERVER_ERROR.name();

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
            } else if (throwable instanceof SecurityException) {
                handleSecurityException(message, channel, (SecurityException) throwable);
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

    private void handleSecurityException(final Delivery message, final Channel channel, final SecurityException exception)
            throws IOException {
        final var originalExchange = message.getEnvelope().getExchange();
        final var originalRoutingKey = message.getEnvelope().getRoutingKey();
        log.error(String.format(
                "Authentication failed, message with given binding keys(s) is being discarded (acknowledged). error: '%s', exchange: '%s', routingKey: '%s'",
                exception.getClientCode(), originalExchange, originalRoutingKey), exception);

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

        log.error("Encountered server exception while processing message. Sending to retry exchange.", throwable);
        acknowledgeMessage(channel, message);
        retryEnqueuer.enqueueWithBackoff(irisContext, message, exception, exception.shouldNotifyFrontend());
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

    public static SecurityException getSecurityException(java.lang.SecurityException securityException) {
        // TODO: change with switch once available as non-preview
        final var message = securityException.getMessage();
        final var cause = securityException.getCause();

        if (securityException instanceof AuthenticationFailedException) {
            return new id.global.iris.common.exception.AuthenticationFailedException(AUTHENTICATION_FAILED_CLIENT_CODE,
                    message,
                    cause);
        } else if (securityException instanceof ForbiddenException) {
            return new id.global.iris.common.exception.ForbiddenException(FORBIDDEN_CLIENT_CODE, message, cause);
        } else if (securityException instanceof UnauthorizedException) {
            return new id.global.iris.common.exception.UnauthorizedException(UNAUTHORIZED_CLIENT_CODE, message, cause);
        }

        return new id.global.iris.common.exception.AuthenticationFailedException(AUTHENTICATION_FAILED_CLIENT_CODE,
                message,
                cause);
    }
}
