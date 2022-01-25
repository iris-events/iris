package id.global.event.messaging.it.sync;

import static id.global.event.messaging.runtime.consumer.AmqpConsumer.ERROR_MESSAGE_EXCHANGE;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.rabbitmq.client.AMQP;

import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.MessageHandler;
import id.global.common.headers.amqp.MessageHeaders;
import id.global.event.messaging.it.AbstractIntegrationTest;
import id.global.event.messaging.it.auth.TokenUtils;
import id.global.event.messaging.runtime.api.error.ClientError;
import id.global.event.messaging.runtime.api.error.ServerError;
import id.global.event.messaging.runtime.api.exception.BadMessageException;
import id.global.event.messaging.runtime.api.exception.ServerException;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ErrorQueueIT extends AbstractIntegrationTest {

    private static final String ERROR_QUEUE_BAD_REQUEST = "error-queue-bad-request";
    private static final String ERROR_QUEUE_SERVER_ERROR = "error-queue-server-error";

    @Override
    public String getErrorMessageQueue() {
        return "error-message-error-queue-it";
    }

    @BeforeEach
    void setUp() throws IOException {
        final var connection = rabbitMQClient.connect("ErrorQueueIT publisher");
        channel = connection.createChannel();
        final var errorMessageQueue = getErrorMessageQueue();
        channel.queueDeclare(errorMessageQueue, false, false, false, emptyMap());
        channel.queueBind(errorMessageQueue, ERROR_MESSAGE_EXCHANGE, ERROR_QUEUE_BAD_REQUEST + ".error");
        channel.queueBind(errorMessageQueue, ERROR_MESSAGE_EXCHANGE, ERROR_QUEUE_SERVER_ERROR + ".error");
    }

    @DisplayName("Send bad request error message on corrupted message")
    @Test
    void corruptedMessage() throws Exception {
        final var token = TokenUtils.generateTokenString("/AuthenticatedToken.json");
        final var message = new BadRequestMessage(UUID.randomUUID().toString());
        final var basicProperties = new AMQP.BasicProperties().builder()
                .headers(Map.of(MessageHeaders.JWT, token))
                .build();

        channel.basicPublish(ERROR_QUEUE_BAD_REQUEST, ERROR_QUEUE_BAD_REQUEST, basicProperties,
                objectMapper.writeValueAsBytes(message));

        final var errorMessage = getErrorResponse(5);
        assertThat(errorMessage, is(notNullValue()));
        assertThat(errorMessage.name(), is(ClientError.BAD_REQUEST.getName()));
        assertThat(errorMessage.message(), is("Unable to process message. Message corrupted."));
    }

    @DisplayName("Send server error message on server exception")
    @Test
    void serverException() throws Exception {
        final var token = TokenUtils.generateTokenString("/AuthenticatedToken.json");
        final var message = new ServerErrorMessage(UUID.randomUUID().toString());
        final var basicProperties = new AMQP.BasicProperties().builder()
                .headers(Map.of(MessageHeaders.JWT, token))
                .build();

        channel.basicPublish(ERROR_QUEUE_SERVER_ERROR, ERROR_QUEUE_SERVER_ERROR, basicProperties,
                objectMapper.writeValueAsBytes(message));

        final var errorMessage = getErrorResponse(5);
        assertThat(errorMessage, is(notNullValue()));
        assertThat(errorMessage.name(), is(ServerError.SERVER_ERROR.name()));
        assertThat(errorMessage.message(), is("Internal service error."));
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class ErrorQueueService {

        @Inject
        public ErrorQueueService() {
        }

        @MessageHandler
        public void handle(BadRequestMessage message) {
            throw new BadMessageException(ClientError.BAD_REQUEST, "Unable to process message. Message corrupted.");
        }

        @MessageHandler
        public void handle(ServerErrorMessage message) {
            throw new ServerException(ServerError.SERVER_ERROR, "Internal service error.", true);
        }
    }

    @Message(name = ERROR_QUEUE_BAD_REQUEST)
    public record BadRequestMessage(String name) {
    }

    @Message(name = ERROR_QUEUE_SERVER_ERROR)
    public record ServerErrorMessage(String name) {
    }
}
