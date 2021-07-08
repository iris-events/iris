package id.global.event.messaging.it;

import id.global.asyncapi.spec.annotations.FanoutMessageHandler;
import id.global.event.messaging.runtime.configuration.AmqpConfiguration;
import id.global.event.messaging.runtime.producer.AmqpProducer;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FanoutExchangeConsumeIT {
    @Inject AmqpProducer producer;

    @Inject MyLoggingServiceA internalLoggingServiceA;

    @Inject MyLoggingServiceB internalLoggingServiceB;

    @Inject AmqpConfiguration config;

    private static final String EXCHANGE = "test_fanout_exchange";

    // For proper cleanup we need names of all "random" queue names generated for fanout exchanges
    //    @AfterAll void teardown() throws IOException, TimeoutException {
    //        ConnectionFactory factory = new ConnectionFactory();
    //        factory.setHost(config.getUrl());
    //        factory.setPort(config.getPort());
    //
    //        if (config.isAuthenticated()) {
    //            factory.setUsername(config.getUsername());
    //            factory.setPassword(config.getPassword());
    //        }
    //        factory.setAutomaticRecoveryEnabled(true);
    //        Connection connection = factory.newConnection();
    //        Channel channel = connection.createChannel();
    //
    ////        channel.queueDelete(EVENT_QUEUE);
    ////        channel.queueDelete(EVENT_QUEUE_PRIORITY);
    //
    //        // temp
    //        channel.exchangeDelete(EXCHANGE);
    //
    //        channel.close();
    //        connection.close();
    //    }

    @Test
    void fanoutTest() throws IOException, InterruptedException, ExecutionException {
        producer.declareFanoutExchange(EXCHANGE);
        producer.sendMessage(new LoggingEvent("this is log", 1L), EXCHANGE, "");

        assertEquals("this is log", internalLoggingServiceA.getFuture().get());
        assertEquals("this is log", internalLoggingServiceB.getFuture().get());
    }

    @ApplicationScoped
    public static class MyLoggingServiceA {
        public CompletableFuture<String> future = new CompletableFuture<>();

        @FanoutMessageHandler(exchange = EXCHANGE)
        public void handleLogEvents(LoggingEvent event) {
            future.complete(event.getLog());
        }

        public CompletableFuture<String> getFuture() {
            return future;
        }
    }

    @ApplicationScoped
    public static class MyLoggingServiceB {
        public CompletableFuture<String> future = new CompletableFuture<>();

        @FanoutMessageHandler(exchange = EXCHANGE)
        public void handleLogEvents(LoggingEvent event) {
            future.complete(event.getLog());
        }

        public CompletableFuture<String> getFuture() {
            return future;
        }
    }

    public static class LoggingEvent {

        private String log;
        private Long level;

        public LoggingEvent() {
        }

        public LoggingEvent(String log, Long level) {
            this.log = log;
            this.level = level;
        }

        public String getLog() {
            return log;
        }

        public Long getLevel() {
            return level;
        }

        public void setLog(String log) {
            this.log = log;
        }

        public void setLevel(Long level) {
            this.level = level;
        }
    }
}
