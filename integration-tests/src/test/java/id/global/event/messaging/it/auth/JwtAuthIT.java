package id.global.event.messaging.it.auth;

import static id.global.event.messaging.runtime.consumer.AmqpConsumer.ERROR_MESSAGE_EXCHANGE;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.rabbitmq.client.AMQP;

import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.MessageHandler;
import id.global.common.headers.amqp.MessageHeaders;
import id.global.event.messaging.it.AbstractIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JwtAuthIT extends AbstractIntegrationTest {

    public static final String JWT_AUTH_MESSAGE = "jwt-auth-message";

    @Inject
    JwtTestService service;

    @Override
    public String getErrorMessageQueue() {
        return "error-message-jwt-aut-it";
    }

    @BeforeEach
    void setUp() throws IOException {
        final var connection = rabbitMQClient.connect("JwtAuthIT publisher");
        channel = connection.createChannel(ThreadLocalRandom.current().nextInt(0, 1000));
        final var errorMessageQueue = getErrorMessageQueue();
        channel.queueDeclare(errorMessageQueue, false, false, false, emptyMap());
        channel.queueBind(errorMessageQueue, ERROR_MESSAGE_EXCHANGE, JWT_AUTH_MESSAGE);
    }

    @DisplayName("Resolve valid JWT")
    @Test
    void resolveJwt() throws Exception {
        final var token = TokenUtils.generateTokenString("/Token1.json");
        final var message = new JwtAuthMessage(UUID.randomUUID().toString());
        final var basicProperties = new AMQP.BasicProperties().builder()
                .headers(Map.of(MessageHeaders.JWT, token))
                .build();

        channel.basicPublish(JWT_AUTH_MESSAGE, JWT_AUTH_MESSAGE, basicProperties, objectMapper.writeValueAsBytes(message));

        final var jwtSubject = service.getJwtSubject().get(5, TimeUnit.SECONDS);
        assertThat(jwtSubject, is("a777fa3d-6ff7-401f-94d0-e708e80619ad"));
    }

    @DisplayName("Throw exception on non parseable JWT")
    @Test
    void nonParseableJwt() throws Exception {
        final var token = TokenUtils.generateTokenString("/Token1.json");
        final var nonParseableToken = token + "some_suffix";
        final var message = new JwtAuthMessage(UUID.randomUUID().toString());
        final var basicProperties = new AMQP.BasicProperties().builder()
                .headers(Map.of(MessageHeaders.JWT, nonParseableToken))
                .build();

        channel.basicPublish(JWT_AUTH_MESSAGE, JWT_AUTH_MESSAGE, basicProperties, objectMapper.writeValueAsBytes(message));

        final var errorMessage = getErrorResponse(5);
        assertThat(errorMessage, is(notNullValue()));
        assertThat(errorMessage.error(), is("AUTHORIZATION_FAILED"));
        assertThat(errorMessage.message(), is("Invalid authorization token"));
    }

    @DisplayName("Throw exception on invalid claim")
    @ParameterizedTest
    @EnumSource(value = TokenUtils.InvalidClaims.class, names = { "EXP", "ISSUER", "SIGNER", "ALG" })
    void expiredToken() throws Exception {
        final var token = TokenUtils.generateTokenString("/Token1.json", Set.of(TokenUtils.InvalidClaims.EXP));
        final var message = new JwtAuthMessage(UUID.randomUUID().toString());
        final var basicProperties = new AMQP.BasicProperties().builder()
                .headers(Map.of(MessageHeaders.JWT, token))
                .build();

        channel.basicPublish(JWT_AUTH_MESSAGE, JWT_AUTH_MESSAGE, basicProperties, objectMapper.writeValueAsBytes(message));

        final var errorMessage = getErrorResponse(5);
        assertThat(errorMessage, is(notNullValue()));
        assertThat(errorMessage.error(), is("AUTHORIZATION_FAILED"));
        assertThat(errorMessage.message(), is("Invalid authorization token"));
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class JwtTestService {
        private final JsonWebToken jsonWebToken;
        private final CompletableFuture<String> jwtSubCompletableFuture = new CompletableFuture<>();

        @Inject
        public JwtTestService(JsonWebToken jsonWebToken) {
            this.jsonWebToken = jsonWebToken;
        }

        @MessageHandler
        public void handle(JwtAuthMessage message) {
            jwtSubCompletableFuture.complete(jsonWebToken.getSubject());
        }

        public CompletableFuture<String> getJwtSubject() {
            return jwtSubCompletableFuture;
        }
    }

    @Message(name = JWT_AUTH_MESSAGE)
    public record JwtAuthMessage(String name) {
    }
}
