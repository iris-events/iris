package io.smallrye.asyncapi.runtime.scanner.app;

import java.util.Map;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import id.global.asyncapi.spec.annotations.ConsumedEvent;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.enums.ExchangeType;
import io.smallrye.asyncapi.runtime.scanner.model.User;
import io.smallrye.asyncapi.spec.annotations.EventApp;
import io.smallrye.asyncapi.spec.annotations.info.Info;

@EventApp(id = EventHandlersApp.ID, info = @Info(title = EventHandlersApp.TITLE, version = EventHandlersApp.VERSION))
public class EventHandlersApp {
    private static final Logger LOG = Logger.getLogger(EventHandlersApp.class);

    public static final String TITLE = "Event handlers";
    public static final String VERSION = "1.0.0";
    public static final String ID = "EventHandlersAppTest";

    @MessageHandler
    public void handleEventV1(TestEventV1 event) {
        LOG.info("Handle event: " + event);
    }

    @MessageHandler
    public void handleEventV1Params(TestEventV2 event) {
        LOG.info("Handle event: " + event + " with JsonNode and Map");
    }

    @MessageHandler
    public void handleFrontendEvent(FrontendTestEventV1 event) {
        LOG.info("Handle event: " + event);
    }

    @MessageHandler
    public void handleTopicEvent(TopicTestEventV1 event) {
        LOG.info("Handling topic event");
    }

    @MessageHandler
    public void handleFanoutEvent(FanoutTestEventV1 event) {
        LOG.info("Handling fanout event");
    }

    @ConsumedEvent(queue = "default-test-event-v1")
    public record TestEventV1(int id, String status, User user) {
    }

    @ConsumedEvent(queue = "test-event-v2")
    public record TestEventV2(
            int id,
            String name,
            String surname,
            User user,
            JsonNode payload,
            Map<String, String> someMap) {
    }

    @ConsumedEvent(queue = "fe-test-event-v1")
    public record FrontendTestEventV1(int id, String status, User user) {
    }

    @ConsumedEvent(exchange = "test-topic-exchange", exchangeType = ExchangeType.TOPIC, bindingKeys = { "*.*.rabbit",
            "fast.orange.*" })
    public record TopicTestEventV1(int id, String status, User user) {
    }

    @ConsumedEvent(exchange = "test-fanout-exchange", exchangeType = ExchangeType.FANOUT)
    public record FanoutTestEventV1(int id, String status, User user) {
    }
}
