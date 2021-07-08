package id.global.event.messaging.it;

import id.global.asyncapi.spec.annotations.MessageHandler;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProduceAndConsumeIT {
    public static final String EVENT_PAYLOAD_NAME = "name";
    public static final long EVENT_PAYLOAD_AGE = 10L;
    public static final String EVENT_QUEUE = "test_EventQueue";
    public static final String EVENT_QUEUE_PRIORITY = "test_EventQueue_priority";
    public static final String EXCHANGE = "test_exchange";

    @Inject AmqpProducer producer;

    @Inject TestHandlerService service;

    @Inject AmqpConfiguration config;

    // Won't be cleaning rabbitmq just now, because quarkus initializes all beans, not just thoe used in this test
    // this means that also queues and exchanges implied by other services get created and those we can not clean here
    //

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
    //        channel.queueDelete(EVENT_QUEUE);
    //        channel.queueDelete(EVENT_QUEUE_PRIORITY);
    //
    //        // temp
    //        channel.exchangeDelete(EXCHANGE);
    //
    //        channel.close();
    //        connection.close();
    //    }

    @Test
    void basicProduceConsumeTest() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        producer.sendMessage(new Event(EVENT_PAYLOAD_NAME, EVENT_PAYLOAD_AGE), EXCHANGE, EVENT_QUEUE_PRIORITY);

        assertEquals(EVENT_PAYLOAD_NAME, service.getHandledPriorityEvent().get(1, TimeUnit.SECONDS).getName());
        assertEquals(EVENT_PAYLOAD_AGE, service.getHandledPriorityEvent().get(1, TimeUnit.SECONDS).getAge());

        assertThrows(TimeoutException.class, () -> service.getHandledEvent().get(1000, TimeUnit.MILLISECONDS));
    }

    @ApplicationScoped
    public static class TestHandlerService {
        private final CompletableFuture<Event> handledEvent = new CompletableFuture<>();
        private final CompletableFuture<Event> handledPriorityEvent = new CompletableFuture<>();

        @MessageHandler(queue = EVENT_QUEUE, exchange = EXCHANGE)
        public void handle(Event event) {
            handledEvent.complete(event);
        }

        @MessageHandler(queue = EVENT_QUEUE_PRIORITY, exchange = EXCHANGE)
        public void handlePriority(Event event) {
            handledPriorityEvent.complete(event);
        }

        public CompletableFuture<Event> getHandledEvent() {
            return handledEvent;
        }

        public CompletableFuture<Event> getHandledPriorityEvent() {
            return handledPriorityEvent;
        }
    }

    public static class Event {

        private String name;
        private Long age;

        public Event() {
        }

        public Event(String name, Long age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public Long getAge() {
            return age;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setAge(Long age) {
            this.age = age;
        }
    }
}
