package org.iris_events.asyncapi.runtime.client;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.iris_events.annotations.Scope;
import org.iris_events.asyncapi.api.AsyncApiConstants;
import org.iris_events.asyncapi.runtime.io.channel.operation.OperationConstant;
import org.iris_events.asyncapi.runtime.scanner.model.ClientDefinitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public class ClientDefinitionParser {

    private static final Logger log = LoggerFactory.getLogger(ClientDefinitionParser.class);

    public ClientDefinitions parse(JsonNode asyncapiRootNode) {
        EnumSet<Scope> scopes = EnumSet.of(Scope.FRONTEND, Scope.USER, Scope.SESSION, Scope.BROADCAST);

        Map<String, JsonNode> feScopeChannels = getChannelsInScope(asyncapiRootNode.get(AsyncApiConstants.CHANNELS_NODE),
                scopes);
        Map<String, JsonNode> schemasNodes = getTopLevelSchemaNodes(asyncapiRootNode.get(AsyncApiConstants.COMPONENTS_NODE).get(
                AsyncApiConstants.SCHEMAS_NODE),
                feScopeChannels);

        schemasNodes.putAll(getSchemaNodes(getReferencedSchemaNodes(schemasNodes, asyncapiRootNode), asyncapiRootNode));

        return new ClientDefinitions(asyncapiRootNode.findValue(AsyncApiConstants.INFO_NODE).findValue(
                AsyncApiConstants.TITLE_NODE).asText(), feScopeChannels,
                schemasNodes);
    }

    private Map<String, JsonNode> getSchemaNodes(Set<String> referencedSchemaNodes, JsonNode rootNode) {
        JsonNode schemas = rootNode.findPath(AsyncApiConstants.COMPONENTS_NODE).findPath(AsyncApiConstants.SCHEMAS_NODE);
        return referencedSchemaNodes.stream().collect(Collectors.toMap(s -> s, schemas::findPath));
    }

    private Set<String> getReferencedSchemaNodes(Map<String, JsonNode> rootSchemaNodes, JsonNode rootNode) {
        Set<String> refsOfInterest = rootSchemaNodes.values().stream()
                .filter(schemaNode -> schemaNode != null && !schemaNode.findPath(AsyncApiConstants.PROPERTIES_NODE).isEmpty()
                        && !schemaNode.findPath(AsyncApiConstants.PROPERTIES_NODE).findValues(AsyncApiConstants.REF_NODE)
                                .isEmpty())
                .map(nodeOfInterest -> nodeOfInterest.findValues(AsyncApiConstants.REF_NODE))
                .flatMap(List::stream)
                .map(jsonNode -> jsonNode.asText().replace(AsyncApiConstants.COMPONENT_SCHEMAS_PREFIX, ""))
                .collect(Collectors.toSet());

        JsonNode componentSchemas = rootNode.findPath(AsyncApiConstants.COMPONENTS_NODE)
                .findPath(AsyncApiConstants.SCHEMAS_NODE);

        Map<String, JsonNode> newRootSchemaNodes = refsOfInterest.stream()
                .map(ref -> ref.replace(AsyncApiConstants.COMPONENT_SCHEMAS_PREFIX, ""))
                .collect(Collectors.toMap(s -> s, componentSchemas::findPath));

        if (newRootSchemaNodes.isEmpty()) {
            return new HashSet<>();
        }
        refsOfInterest.addAll(getReferencedSchemaNodes(newRootSchemaNodes, rootNode));
        return refsOfInterest;
    }

    private Map<String, JsonNode> getTopLevelSchemaNodes(JsonNode schemasNode, Map<String, JsonNode> feScopeChannels) {
        return feScopeChannels.values().stream()
                .map(scopeChannelNode -> extractRefValue(scopeChannelNode).replace(AsyncApiConstants.COMPONENT_SCHEMAS_PREFIX,
                        ""))
                .collect(Collectors.toMap(s -> s, schemasNode::get, (s1, s2) -> s1));
    }

    private String extractRefValue(JsonNode scopeChannelNode) {
        JsonNode operationNode = Optional.ofNullable(scopeChannelNode.get(OperationConstant.PROP_PUBLISH))
                .orElse(scopeChannelNode.get(OperationConstant.PROP_SUBSCRIBE));
        return operationNode.get(OperationConstant.PROP_MESSAGE).get(AsyncApiConstants.PAYLOAD_NODE).get(
                AsyncApiConstants.REF_NODE).asText();
    }

    private Stream<Map.Entry<String, JsonNode>> fieldsToStream(Iterator<Map.Entry<String, JsonNode>> fields) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(fields, Spliterator.ORDERED), false);
    }

    private Map<String, JsonNode> getChannelsInScope(JsonNode channelsNode, EnumSet<Scope> scopes) {
        return fieldsToStream(channelsNode.fields())
                .filter(channelNode -> {
                    JsonNode operationNode = Optional.ofNullable(channelNode.getValue().get(OperationConstant.PROP_PUBLISH))
                            .orElse(channelNode.getValue().get(OperationConstant.PROP_SUBSCRIBE));

                    var props = operationNode.get(OperationConstant.PROP_MESSAGE).get(AsyncApiConstants.HEADERS_NODE).get(
                            AsyncApiConstants.PROPERTIES_NODE);
                    if (props.hasNonNull(AsyncApiConstants.X_SCOPE_NODE)) {
                        if (props.get(AsyncApiConstants.X_SCOPE_NODE).hasNonNull(AsyncApiConstants.VALUE_NODE)) {
                            String scopeValue = props.get(AsyncApiConstants.X_SCOPE_NODE).get(AsyncApiConstants.VALUE_NODE)
                                    .asText();
                            return scopes.contains(Scope.valueOf(scopeValue.toUpperCase()));
                        } else if (props.hasNonNull("enum")) { //node uses this bit different
                            var scope = props.withArray("enum").get(0).textValue();
                            return scopes.contains(Scope.valueOf(scope.toUpperCase()));
                        }
                    } else {
                        log.warn("Missing scope value for: {}", channelNode.getKey());
                        return false;
                    }
                    return false;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
