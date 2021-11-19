package id.global.asyncapi.runtime.scanner.app;

import static id.global.common.annotations.amqp.ExchangeType.DIRECT;
import static id.global.common.annotations.amqp.ExchangeType.FANOUT;
import static id.global.common.annotations.amqp.ExchangeType.TOPIC;

import java.util.Map;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import id.global.asyncapi.runtime.scanner.model.User;
import id.global.common.annotations.amqp.GlobalIdGenerated;
import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.MessageHandler;
import id.global.asyncapi.spec.annotations.EventApp;
import id.global.asyncapi.spec.annotations.info.Info;

@EventApp(id = EventHandlersApp.ID, info = @Info(title = EventHandlersApp.TITLE, version = EventHandlersApp.VERSION))
public class EventHandlersApp {
    private static final Logger LOG = Logger.getLogger(EventHandlersApp.class);

    public static final String TITLE = "Event handlers";
    public static final String VERSION = "1.0.0";
    public static final String ID = "EventHandlersAppTest";

    @MessageHandler(bindingKeys = "default-test-event-v1")
    public void handleEventV1(TestEventV1 event) {
        LOG.info("Handle event: " + event);
    }

    @MessageHandler(bindingKeys = "test-event-v2")
    public void handleEventV1Params(TestEventV2 event) {
        LOG.info("Handle event: " + event + " with JsonNode and Map");
    }

    @MessageHandler(bindingKeys = "fe-test-event-v1")
    public void handleFrontendEvent(FrontendTestEventV1 event) {
        LOG.info("Handle event: " + event);
    }

    @MessageHandler(bindingKeys = { "*.*.rabbit", "fast.orange.*" })
    public void handleTopicEvent(TopicTestEventV1 event) {
        LOG.info("Handling topic event");
    }

    @MessageHandler
    public void handleFanoutEvent(FanoutTestEventV1 event) {
        LOG.info("Handling fanout event");
    }

    @MessageHandler
    public void handleOutsideGeneratedEvent(GeneratedTestEvent event) {
        LOG.info("Handling event generated in an external service");
    }

    @MessageHandler
    public void handleEventWithDefaults(EventDefaults event) {
        LOG.info("Handling event with generated defaults");
    }

    @Message(exchangeType = DIRECT)
    public record TestEventV1(int id, String status, User user) {
    }

    @Message(exchangeType = DIRECT, ttl = 10000)
    public record TestEventV2(
            int id,
            String name,
            String surname,
            User user,
            JsonNode payload,
            Map<String, String> someMap) {
    }

    @Message(exchangeType = DIRECT)
    public record FrontendTestEventV1(int id, String status, User user) {
    }

    @Message(exchange = "test-topic-exchange", exchangeType = TOPIC)
    public record TopicTestEventV1(int id, String status, User user) {
    }

    @Message(exchange = "test-fanout-exchange", exchangeType = FANOUT)
    public record FanoutTestEventV1(int id, String status, User user) {
    }

    @Message
    public record EventDefaults(int id) {
    }

    @GlobalIdGenerated
    @Message(exchange = "test-generated-exchange", exchangeType = TOPIC)
    public record GeneratedTestEvent(int id, String status) {

    }

    @SuppressWarnings("unused")
    @Message(exchangeType = FANOUT)
    public record ProducedEvent(int id) {
    }
}
