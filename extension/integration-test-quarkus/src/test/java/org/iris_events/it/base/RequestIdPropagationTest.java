package org.iris_events.it.base;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.iris_events.annotations.ExchangeType.DIRECT;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.iris_events.annotations.Message;
import org.iris_events.annotations.MessageHandler;
import org.iris_events.common.MessagingHeaders;
import org.iris_events.context.EventContext;
import org.iris_events.it.TestRestResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RequestIdPropagationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(
                    Service.class,
                    HandledEvent.class,
                    TestRestResource.class));

    private static final String INITIAL_CONSUMING_QUEUE = "rest-initial-consuming-queue";
    private static final String EXCHANGE = "message-propagation-exchange";

    @Inject
    Service originService;

    @Test
    void testRestResourceSendsMessage() throws ExecutionException, InterruptedException, TimeoutException {
        final var restRequestId = "custom-request-id-header";
        RestAssured.given()
                .header(MessagingHeaders.Message.REQUEST_ID, restRequestId)
                .when().get("/test/send")
                .then()
                .statusCode(200);

        final var correlationId = originService.getCorrelationId().get(5, TimeUnit.SECONDS);
        assertThat(correlationId, is(restRequestId));
    }

    @SuppressWarnings("unused")
    @ApplicationScoped
    public static class Service {
        private final CompletableFuture<String> eventCorrelationId = new CompletableFuture<>();
        @Inject
        EventContext eventContext;

        @MessageHandler
        public void handle(HandledEvent event) {
            final var correlationId = eventContext.getCorrelationId();
            eventCorrelationId.complete(correlationId);
        }

        public CompletableFuture<String> getCorrelationId() {
            return eventCorrelationId;
        }
    }

    @Message(routingKey = INITIAL_CONSUMING_QUEUE, name = EXCHANGE, exchangeType = DIRECT)
    public record HandledEvent(String eventPropertyValue) {
    }
}
