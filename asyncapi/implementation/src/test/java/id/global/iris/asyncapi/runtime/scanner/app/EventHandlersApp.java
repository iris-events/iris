package id.global.iris.asyncapi.runtime.scanner.app;

import static id.global.iris.common.annotations.ExchangeType.DIRECT;
import static id.global.iris.common.annotations.ExchangeType.FANOUT;
import static id.global.iris.common.annotations.ExchangeType.TOPIC;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import id.global.common.auth.jwt.Role;
import id.global.iris.asyncapi.runtime.scanner.model.User;
import id.global.iris.asyncapi.spec.annotations.media.Schema;
import id.global.iris.asyncapi.spec.annotations.media.SchemaProperty;
import id.global.iris.common.annotations.IrisGenerated;
import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.MessageHandler;
import id.global.iris.common.annotations.Scope;
import id.global.iris.common.annotations.SnapshotMessageHandler;
import id.global.iris.common.message.SnapshotRequested;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class EventHandlersApp {
    private static final Logger LOG = LoggerFactory.getLogger(EventHandlersApp.class);

    @SuppressWarnings("unused")
    @MessageHandler
    public void handleTestEventWithDocumentation(TestEventWithDocumentation event) {
        LOG.info("Handle event: " + event);
    }

    @SuppressWarnings("unused")
    @MessageHandler
    public void handleTestEventWithRequirements(TestEventWithRequirements event) {
        LOG.info("Handle event: " + event);
    }

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

    @SnapshotMessageHandler(resourceType = "inventory")
    public void handleSnapshotRequested(SnapshotRequested snapshotRequested) {
        LOG.info("Handle snapshot requested event: " + snapshotRequested);
    }

    @SnapshotMessageHandler(resourceType = "inventory", rolesAllowed = { Role.ADMIN_REWARD, Role.ADMIN_MERCHANT })
    public void handleSnapshotRequestedWithRoles(SnapshotRequested snapshotRequested) {
        LOG.info("Handle snapshot requested event: " + snapshotRequested);
    }

    @Message(name = "test-event-with-documentation", exchangeType = DIRECT, persistent = true)
    @Schema(implementation = TestEventWithDocumentation.class, description = "Event with extensive documentation for test purposes")
    public record TestEventWithDocumentation(
            @SchemaProperty(name = "id", minimum = "5", maximum = "100", exclusiveMaximum = true)
            int id,
            @Min(value = 18, message = "Age should not be less than 18")
            @Max(value = 150, message = "Age should not be greater than 150")
            @SchemaProperty(description = "Alternative event id")
            int altId,
            @SchemaProperty(name = "status",
                    description = "status of the user entity",
                    enumeration = { "available", "pending", "sold" })
            String status,
            User user) {
    }

    @Message(name = "test-event-with-requirements", exchangeType = DIRECT, persistent = true)
    public record TestEventWithRequirements(@jakarta.validation.constraints.NotNull int id,
                                            @javax.validation.constraints.NotNull String status,
                                            User user) {
    }

    @Message(name = "test-event-v1", exchangeType = DIRECT, rolesAllowed = { Role.ADMIN_REWARD,
            Role.ADMIN_MERCHANT }, persistent = true)
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

    @IrisGenerated
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
