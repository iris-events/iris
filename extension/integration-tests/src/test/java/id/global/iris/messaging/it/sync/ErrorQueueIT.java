package id.global.iris.messaging.it.sync;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;

import com.rabbitmq.client.BuiltinExchangeType;

import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.MessageHandler;
import id.global.iris.common.constants.Exchanges;
import id.global.iris.common.error.ErrorType;
import id.global.iris.common.exception.BadPayloadException;
import id.global.iris.common.exception.MessagingException;
import id.global.iris.common.exception.ServerException;
import id.global.iris.messaging.it.AbstractIntegrationTest;
import id.global.iris.messaging.runtime.producer.AmqpProducer;
import id.global.iris.messaging.runtime.requeue.MessageRequeueHandler;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ErrorQueueIT extends AbstractIntegrationTest {

    private static final String ERROR_QUEUE_BAD_REQUEST = "error-queue-bad-request";
    private static final String ERROR_QUEUE_SERVER_ERROR = "error-queue-server-error";
    private static final String ERROR_EXCHANGE = Exchanges.ERROR.getValue();
    private static final String INTERNAL_SERVICE_ERROR = "Internal service error.";
    private static final String BAD_PAYLOAD_CLIENT_CODE = "BAD_MESSAGE_PAYLOAD";
    private static final String SERVER_ERROR_CLIENT_CODE = "SERVER_ERROR";

    @Inject
    AmqpProducer producer;

    @InjectMock
    MessageRequeueHandler requeueHandler;

    @Override
    public String getErrorMessageQueue() {
        return "error-message-error-queue-it";
    }

    @BeforeEach
    void setUp() throws IOException {
        final var connection = rabbitMQClient.connect("ErrorQueueIT publisher");
        channel = connection.createChannel();
        channel.exchangeDeclare(ERROR_EXCHANGE, BuiltinExchangeType.TOPIC, true);
        final var errorMessageQueue = getErrorMessageQueue();
        channel.queueDeclare(errorMessageQueue, false, false, false, emptyMap());
        channel.queueBind(errorMessageQueue, ERROR_EXCHANGE, ERROR_QUEUE_BAD_REQUEST + ".error");
        channel.queueBind(errorMessageQueue, ERROR_EXCHANGE, ERROR_QUEUE_SERVER_ERROR + ".error");
    }

    @DisplayName("Send bad request error message on corrupted message")
    @Test
    void corruptedMessage() throws Exception {
        final var message = new BadRequestMessage(UUID.randomUUID().toString());

        producer.send(message);

        final var errorMessage = getErrorResponse(5);
        assertThat(errorMessage, is(notNullValue()));
        assertThat(errorMessage.code(), is(BAD_PAYLOAD_CLIENT_CODE));
        assertThat(errorMessage.message(), is("Unable to process message. Message corrupted."));
    }

    @DisplayName("Requeue server error message on server exception")
    @Test
    void serverException() throws Exception {
        final var message = new ServerErrorMessage(UUID.randomUUID().toString());

        producer.send(message);

        final var messagingExceptionArgumentCaptor = ArgumentCaptor.forClass(MessagingException.class);
        final var notifyFrontendCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(requeueHandler, timeout(500).times(1))
                .enqueueWithBackoff(any(), any(), messagingExceptionArgumentCaptor.capture(),
                        notifyFrontendCaptor.capture());

        final var messagingException = messagingExceptionArgumentCaptor.getValue();
        final var errorCode = messagingException.getClientCode();
        final var type = messagingException.getErrorType();
        final var exceptionMessage = messagingException.getMessage();
        final var notifyFrontend = notifyFrontendCaptor.getValue();

        assertThat(errorCode, is(notNullValue()));
        assertThat(errorCode, is(SERVER_ERROR_CLIENT_CODE));
        assertThat(type, is(ErrorType.INTERNAL_SERVER_ERROR));
        assertThat(exceptionMessage, is(INTERNAL_SERVICE_ERROR));
        assertThat(notifyFrontend, CoreMatchers.is(true));
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class ErrorQueueService {

        @Inject
        public ErrorQueueService() {
        }

        @MessageHandler
        public void handle(BadRequestMessage message) {
            throw new BadPayloadException(BAD_PAYLOAD_CLIENT_CODE, "Unable to process message. Message corrupted.");
        }

        @MessageHandler
        public void handle(ServerErrorMessage message) {
            throw new ServerException(SERVER_ERROR_CLIENT_CODE, INTERNAL_SERVICE_ERROR, true);
        }
    }

    @Message(name = ERROR_QUEUE_BAD_REQUEST)
    public record BadRequestMessage(String name) {
    }

    @Message(name = ERROR_QUEUE_SERVER_ERROR)
    public record ServerErrorMessage(String name) {
    }
}
