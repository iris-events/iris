package org.iris_events.asyncapi.runtime.scanner.model;

import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public class ClientDefinitions {
    private final String serviceName;
    private final Map<String, JsonNode> clientChannelNodes;
    private final Map<String, JsonNode> schemasNodes;

    public ClientDefinitions(String serviceName, Map<String, JsonNode> clientChannelNodes, Map<String, JsonNode> schemasNodes) {
        this.serviceName = serviceName.toLowerCase(Locale.ROOT).replaceAll(" ", "-");
        this.clientChannelNodes = clientChannelNodes;
        this.schemasNodes = schemasNodes;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Map<String, JsonNode> getClientChannelNodes() {
        return clientChannelNodes;
    }

    public Map<String, JsonNode> getSchemasNodes() {
        return schemasNodes;
    }
}
