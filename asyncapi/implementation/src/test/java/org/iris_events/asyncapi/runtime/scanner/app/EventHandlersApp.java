package org.iris_events.asyncapi.runtime.scanner.app;

import static org.iris_events.annotations.ExchangeType.DIRECT;
import static org.iris_events.annotations.ExchangeType.FANOUT;
import static org.iris_events.annotations.ExchangeType.TOPIC;

import java.util.List;
import java.util.Map;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.iris_events.annotations.CachedMessage;
import org.iris_events.annotations.IrisGenerated;
import org.iris_events.annotations.Message;
import org.iris_events.annotations.MessageHandler;
import org.iris_events.annotations.Scope;
import org.iris_events.annotations.SnapshotMessageHandler;
import org.iris_events.asyncapi.runtime.scanner.model.User;
import org.iris_events.asyncapi.spec.annotations.media.Schema;
import org.iris_events.asyncapi.spec.annotations.media.SchemaProperty;
import org.iris_events.common.message.SnapshotRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

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
    @MessageHandler(bindingKeys = "default-test-event-v1", rolesAllowed = @RolesAllowed({ "admin.reward", "**" }))
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

    @SuppressWarnings("unused")
    @MessageHandler
    public void handleListPayloadEvent(ListPayloadEvent event) {
        LOG.info("Handling event with list payload");
    }

    @SuppressWarnings("unused")
    @MessageHandler
    public RpcResponseEvent handleRpcRequest(RpcRequestEvent rpcReq) {
        LOG.info("Got rpc request. Returning response");
        return new RpcResponseEvent(rpcReq.rpcId);
    }

    @SnapshotMessageHandler(resourceType = "inventory")
    public void handleSnapshotRequested(SnapshotRequested snapshotRequested) {
        LOG.info("Handle snapshot requested event: " + snapshotRequested);
    }

    @SnapshotMessageHandler(resourceType = "inventory", rolesAllowed = @RolesAllowed({ "admin.reward", "admin.merchant" }))
    public void handleSnapshotRequestedWithRoles(SnapshotRequested snapshotRequested) {
        LOG.info("Handle snapshot requested event: " + snapshotRequested);
    }

    @Message(name = "test-event-with-documentation", exchangeType = DIRECT, persistent = true)
    @Schema(implementation = TestEventWithDocumentation.class, description = "Event with extensive documentation for test purposes")
    public record TestEventWithDocumentation(
            @SchemaProperty(name = "id", minimum = "5", maximum = "100", exclusiveMaximum = true) int id,
            @Min(value = 18, message = "Age should not be less than 18") @Max(value = 150, message = "Age should not be greater than 150") @SchemaProperty(description = "Alternative event id") int altId,
            @SchemaProperty(name = "status", description = "status of the user entity", enumeration = {
                    "available", "pending", "sold" }) String status,
            User user){
    }

    @Message(name = "test-event-with-requirements", exchangeType = DIRECT, persistent = true)
    public record TestEventWithRequirements(@NotNull int id,
            @NotNull String status,
            User user) {
    }

    @Message(name = "namespace/test-event-v1", exchangeType = DIRECT, rolesAllowed = @RolesAllowed({ "admin.award",
            "admin.merchant" }), persistent = true)
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

    @CachedMessage(ttl = 100)
    @Message(name = "cached-message")
    public record CachedEvent(int id) {
    }

    @Message(name = "rpc-request", rpcResponse = RpcResponseEvent.class)
    public record RpcRequestEvent(String rpcId) {

    }

    @Message(name = "rpc-response")
    public record RpcResponseEvent(String rpcId) {

    }
}
