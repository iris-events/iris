package id.global.iris.messaging.it.auth;

import static id.global.iris.messaging.runtime.exception.IrisExceptionHandler.AUTHENTICATION_FAILED_CLIENT_CODE;
import static id.global.iris.messaging.runtime.exception.IrisExceptionHandler.FORBIDDEN_CLIENT_CODE;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;

import id.global.common.auth.jwt.Role;
import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.MessageHandler;
import id.global.iris.common.constants.Exchanges;
import id.global.iris.common.constants.MessagingHeaders;
import id.global.iris.messaging.it.AbstractIntegrationTest;
import id.global.iris.messaging.runtime.channel.ChannelService;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JwtAuthIT extends AbstractIntegrationTest {

    public static final String JWT_AUTH_MESSAGE = "jwt-auth-message";
    public static final String JWT_ROLE_SECURED_HANDLER_MESSAGE = "jwt-role-secured-handler";
    private static final String ERROR_EXCHANGE = Exchanges.ERROR.getValue();

    @Inject
    JwtTestService service;

    @Inject
    @Named("consumerChannelService")
    ChannelService channelService;

    @Override
    public String getErrorMessageQueue() {
        return "error-message-jwt-aut-it";
    }

    @BeforeEach
    void setUp() throws IOException {
        channel = channelService.createChannel();
        channel.exchangeDeclare(ERROR_EXCHANGE, BuiltinExchangeType.TOPIC, true);
        final var errorMessageQueue = getErrorMessageQueue();
        channel.queueDeclare(errorMessageQueue, false, false, false, emptyMap());
        channel.queueBind(errorMessageQueue, ERROR_EXCHANGE, JWT_AUTH_MESSAGE + ".error");
        channel.queueBind(errorMessageQueue, ERROR_EXCHANGE, JWT_ROLE_SECURED_HANDLER_MESSAGE + ".error");
    }

    @DisplayName("Resolve valid JWT")
    @Test
    void resolveJwt() throws Exception {
        final var token = TokenUtils.generateTokenString("/AuthenticatedToken.json");
        final var message = new JwtAuthMessage(UUID.randomUUID().toString());
        final var basicProperties = new AMQP.BasicProperties().builder()
                .headers(Map.of(MessagingHeaders.Message.JWT, token))
                .build();

        channel.basicPublish(JWT_AUTH_MESSAGE, JWT_AUTH_MESSAGE, basicProperties, objectMapper.writeValueAsBytes(message));

        final var jwtSubject = service.getJwtSubject().get(5, TimeUnit.SECONDS);
        assertThat(jwtSubject, is("a777fa3d-6ff7-401f-94d0-e708e80619ad"));
    }

    @DisplayName("Throw exception on non parseable JWT")
    @Test
    void nonParseableJwt() throws Exception {
        final var token = TokenUtils.generateTokenString("/AuthenticatedToken.json");
        final var nonParseableToken = token + "some_suffix";
        final var message = new JwtAuthMessage(UUID.randomUUID().toString());
        final var basicProperties = new AMQP.BasicProperties().builder()
                .headers(Map.of(MessagingHeaders.Message.JWT, nonParseableToken))
                .build();

        channel.basicPublish(JWT_AUTH_MESSAGE, JWT_AUTH_MESSAGE, basicProperties, objectMapper.writeValueAsBytes(message));

        final var errorMessage = getErrorResponse(5);
        assertThat(errorMessage, is(notNullValue()));
        assertThat(errorMessage.code(), is(AUTHENTICATION_FAILED_CLIENT_CODE));
        assertThat(errorMessage.message(), is("Invalid authorization token"));
    }

    @DisplayName("Consume on role protected handler")
    @Test
    void roleProtectedHandler() throws Exception {
        final var token = TokenUtils.generateTokenString("/GroupOwnerToken.json");
        final var message = new JwtAuthMessage(UUID.randomUUID().toString());
        final var basicProperties = new AMQP.BasicProperties().builder()
                .headers(Map.of(MessagingHeaders.Message.JWT, token))
                .build();

        channel.basicPublish(JWT_AUTH_MESSAGE, JWT_AUTH_MESSAGE, basicProperties, objectMapper.writeValueAsBytes(message));

        final var jwtSubject = service.getJwtSubject().get(5, TimeUnit.SECONDS);
        assertThat(jwtSubject, is("a777fa3d-6ff7-401f-94d0-e708e80619ad"));
    }

    @DisplayName("Throw exception on missing role")
    @Test
    void missingRole() throws Exception {
        final var token = TokenUtils.generateTokenString("/AuthenticatedToken.json");
        final var message = new JwtRoleSecuredHandlerMessage(UUID.randomUUID().toString());
        final var basicProperties = new AMQP.BasicProperties().builder()
                .headers(Map.of(MessagingHeaders.Message.JWT, token))
                .build();

        channel.basicPublish(JWT_ROLE_SECURED_HANDLER_MESSAGE, JWT_ROLE_SECURED_HANDLER_MESSAGE, basicProperties,
                objectMapper.writeValueAsBytes(message));

        final var errorMessage = getErrorResponse(5);
        assertThat(errorMessage, is(notNullValue()));
        assertThat(errorMessage.code(), is(FORBIDDEN_CLIENT_CODE));
        assertThat(errorMessage.message(), is("Role is not allowed"));
    }

    @DisplayName("Throw exception on invalid claim")
    @ParameterizedTest
    @EnumSource(value = TokenUtils.InvalidClaims.class, names = { "EXP", "ISSUER", "SIGNER", "ALG" })
    void expiredToken() throws Exception {
        final var token = TokenUtils.generateTokenString("/AuthenticatedToken.json", Set.of(TokenUtils.InvalidClaims.EXP));
        final var message = new JwtAuthMessage(UUID.randomUUID().toString());
        final var basicProperties = new AMQP.BasicProperties().builder()
                .headers(Map.of(MessagingHeaders.Message.JWT, token))
                .build();

        channel.basicPublish(JWT_AUTH_MESSAGE, JWT_AUTH_MESSAGE, basicProperties, objectMapper.writeValueAsBytes(message));

        final var errorMessage = getErrorResponse(5);
        assertThat(errorMessage, is(notNullValue()));
        assertThat(errorMessage.code(), is(AUTHENTICATION_FAILED_CLIENT_CODE));
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

        @MessageHandler(rolesAllowed = { Role.GROUP_OWNER })
        public void handle(JwtRoleSecuredHandlerMessage message) {
            jwtSubCompletableFuture.complete(jsonWebToken.getSubject());
        }

        public CompletableFuture<String> getJwtSubject() {
            return jwtSubCompletableFuture;
        }
    }

    @Message(name = JWT_AUTH_MESSAGE)
    public record JwtAuthMessage(String name) {
    }

    @Message(name = JWT_ROLE_SECURED_HANDLER_MESSAGE)
    public record JwtRoleSecuredHandlerMessage(String name) {
    }
}
