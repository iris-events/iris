package id.global.iris.asyncapi.runtime.client;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;

import id.global.common.iris.annotations.Scope;
import id.global.iris.asyncapi.api.AsyncApiConstants;
import id.global.iris.asyncapi.runtime.io.channel.operation.OperationConstant;
import id.global.iris.asyncapi.runtime.scanner.model.ClientDefinitions;

public class ClientDefinitionParser {

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
                .collect(Collectors.toMap(s -> s, schemasNode::get));
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
        return fieldsToStream(channelsNode.fields()).filter(channelNode -> {
            JsonNode operationNode = Optional.ofNullable(channelNode.getValue().get(OperationConstant.PROP_PUBLISH))
                    .orElse(channelNode.getValue().get(OperationConstant.PROP_SUBSCRIBE));
            String scopeValue = operationNode.get(OperationConstant.PROP_MESSAGE).get(AsyncApiConstants.HEADERS_NODE).get(
                    AsyncApiConstants.PROPERTIES_NODE)
                    .get(AsyncApiConstants.X_SCOPE_NODE).get(AsyncApiConstants.VALUE_NODE)
                    .asText();
            return scopes.contains(Scope.valueOf(scopeValue.toUpperCase()));
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
