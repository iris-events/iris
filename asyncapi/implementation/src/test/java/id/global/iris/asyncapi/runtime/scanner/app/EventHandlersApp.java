package id.global.iris.asyncapi.runtime.scanner.app;

import static id.global.common.iris.annotations.ExchangeType.DIRECT;
import static id.global.common.iris.annotations.ExchangeType.FANOUT;
import static id.global.common.iris.annotations.ExchangeType.TOPIC;

import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import id.global.common.auth.jwt.Role;
import id.global.common.iris.annotations.GlobalIdGenerated;
import id.global.common.iris.annotations.Message;
import id.global.common.iris.annotations.MessageHandler;
import id.global.common.iris.annotations.Scope;
import id.global.iris.asyncapi.runtime.scanner.model.User;

public class EventHandlersApp {
    private static final Logger LOG = Logger.getLogger(EventHandlersApp.class);

    @SuppressWarnings("unused")
    @MessageHandler(bindingKeys = "default-test-event-v1", rolesAllowed = { Role.ADMIN_REWARD, Role.AUTHENTICATED })
    public void handleEventV1(TestEventV1 event) {
        LOG.info("Handle event: " + event);
    }

    @SuppressWarnings("unused")
    @MessageHandler(bindingKeys = "test-event-v2")
    public void handleEventV1Params(TestEventV2 event) {
        LOG.info("Handle event: " + event + " with JsonNode and Map");
    }

    @SuppressWarnings("unused")
    @MessageHandler(bindingKeys = "fe-test-event-v1")
    public void handleFrontendEvent(FrontendTestEventV1 event) {
        LOG.info("Handle event: " + event);
    }

    @SuppressWarnings("unused")
    @MessageHandler(bindingKeys = { "*.*.rabbit", "fast.orange.*" })
    public void handleTopicEvent(TopicTestEventV1 event) {
        LOG.info("Handling topic event");
    }

    @SuppressWarnings("unused")
    @MessageHandler
    public void handleFanoutEvent(FanoutTestEventV1 event) {
        LOG.info("Handling fanout event");
    }

    @SuppressWarnings("unused")
    @MessageHandler
    public void handleOutsideGeneratedEvent(GeneratedTestEvent event) {
        LOG.info("Handling event generated in an external service");
    }

    @SuppressWarnings("unused")
    @MessageHandler
    public void handleEventWithDefaults(EventDefaults event) {
        LOG.info("Handling event with generated defaults");
    }

    @SuppressWarnings("unused")
    @MessageHandler
    public PassthroughOutboundEvent handleAndPass(PassthroughInboundEvent event) {
        LOG.info("Handling PassthroughInboundEvent");
        return new PassthroughOutboundEvent(666);
    }

    @SuppressWarnings("unused")
    @MessageHandler
    public void handleMapPayloadEvent(MapPayloadEvent event) {
        LOG.info("Handling event with map payload");
    }

    @MessageHandler
    public void handleListPayloadEvent(ListPayloadEvent event) {
        LOG.info("Handling event with list payload");
    }

    @Message(name = "test-event-v1", exchangeType = DIRECT, rolesAllowed = { Role.ADMIN_REWARD, Role.ADMIN_MERCHANT })
    public record TestEventV1(int id, String status, User user) {
    }

    @Message(name = "test-event-v2", exchangeType = DIRECT, ttl = 10000)
    public record TestEventV2(
            int id,
            String name,
            String surname,
            User user,
            JsonNode payload,
            Map<String, String> someMap) {
    }

    @Message(name = "frontend-test-event-v1", exchangeType = DIRECT, scope = Scope.FRONTEND)
    public record FrontendTestEventV1(int id, String status, User user) {
    }

    @Message(name = "test-topic-exchange", exchangeType = TOPIC)
    public record TopicTestEventV1(int id, String status, User user) {
    }

    @Message(name = "test-fanout-exchange", exchangeType = FANOUT)
    public record FanoutTestEventV1(int id, String status, User user) {
    }

    @Message(name = "event-defaults")
    public record EventDefaults(int id) {
    }

    @GlobalIdGenerated
    @Message(name = "test-generated-exchange", exchangeType = TOPIC)
    public record GeneratedTestEvent(int id, String status) {

    }

    // Event only produced, not being used in any handlers
    @SuppressWarnings("unused")
    @Message(name = "produced-event", exchangeType = FANOUT)
    public record ProducedEvent(int id) {
    }

    @Message(name = "passthrough-inbound-event", response = PassthroughOutboundEvent.class)
    public record PassthroughInboundEvent(int id) {
    }

    // Event only returned as a passthrough
    @Message(name = "passthrough-outbound-event")
    public record PassthroughOutboundEvent(int id) {
    }

    @Message(name = "map-payload-event")
    public record MapPayloadEvent(Map<String, MapValue> userMap) {
    }

    public record MapValue(String id, String value) {

    }

    @Message(name = "list-payload-event")
    public record ListPayloadEvent(List<User> userList) {

    }
}
