package id.global.iris.asyncapi.runtime.client;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import id.global.iris.asyncapi.api.AsyncApiConstants;
import id.global.iris.asyncapi.runtime.json.EdaObjectMapper;
import id.global.iris.asyncapi.runtime.scanner.model.ClientDefinitions;

public class ClientDefinitionMerger {

    public JsonNode merge(List<ClientDefinitions> definitions, String version) {
        JsonNode clientAsyncApi = generateBase(version);

        definitions.forEach(def -> {
            setChannelNodes(clientAsyncApi, def);
            setSchemaNodes(clientAsyncApi, def);
        });

        return clientAsyncApi;
    }

    private void nestSchemas(Map<String, JsonNode> schemasNodes, String serviceName) {
        schemasNodes.entrySet().stream()
                .filter(stringJsonNodeEntry -> stringJsonNodeEntry.getValue() != null)
                .forEach(stringJsonNodeEntry -> setServiceSpecificRefs(serviceName, stringJsonNodeEntry));
    }

    private void setServiceSpecificRefs(String serviceName, Map.Entry<String, JsonNode> stringJsonNodeEntry) {
        stringJsonNodeEntry.getValue().findValues(AsyncApiConstants.REF_NODE).forEach(refNode -> {
            if (refNode instanceof TextNode) {
                try {
                    // Stupid immutable text node...
                    Field f = TextNode.class.getDeclaredField("_value");
                    f.setAccessible(true);
                    f.set(refNode, insertServiceName(refNode.asText(), serviceName));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private String insertServiceName(String existingRefValue, String serviceName) {
        String newRefPrefix = AsyncApiConstants.COMPONENT_SCHEMAS_PREFIX + serviceName + "/";
        return existingRefValue.replace(AsyncApiConstants.COMPONENT_SCHEMAS_PREFIX, newRefPrefix);
    }

    private JsonNode generateBase(String version) {
        ObjectMapper objectMapper = EdaObjectMapper.getObjectMapper();
        ObjectNode root = objectMapper.createObjectNode();

        root.put("asyncapi", "2.0.0");
        root.put("id", "urn:id:global:client-api");

        ObjectNode infoNode = objectMapper.createObjectNode();
        infoNode.put("title", "GlobalID Client API");
        infoNode.put("version", version);

        root.set("info", infoNode);
        root.set("channels", objectMapper.createObjectNode());

        ObjectNode componentsNode = objectMapper.createObjectNode();
        componentsNode.set("schemas", objectMapper.createObjectNode());

        root.set("components", componentsNode);

        return root;
    }

    private void setSchemaNodes(JsonNode clientAsyncApi, ClientDefinitions definitions) {
        nestSchemas(definitions.getSchemasNodes(), definitions.getServiceName());
        ObjectNode schemasNode = (ObjectNode) ((ObjectNode) clientAsyncApi.findPath(AsyncApiConstants.COMPONENTS_NODE)
                .findPath(AsyncApiConstants.SCHEMAS_NODE)).set(
                        definitions.getServiceName(), EdaObjectMapper.getObjectMapper().createObjectNode())
                .findPath(definitions.getServiceName());
        definitions.getSchemasNodes().forEach(schemasNode::set);
    }

    private void setChannelNodes(JsonNode clientAsyncApi, ClientDefinitions definitions) {
        nestSchemas(definitions.getClientChannelNodes(),
                definitions.getServiceName());
        ObjectNode channelsNode = (ObjectNode) clientAsyncApi.findPath(AsyncApiConstants.CHANNELS_NODE);
        definitions.getClientChannelNodes().forEach(channelsNode::set);
    }
}
