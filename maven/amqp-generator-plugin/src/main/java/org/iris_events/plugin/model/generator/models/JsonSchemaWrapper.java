package org.iris_events.plugin.model.generator.models;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonSchemaWrapper {

    private final String className;
    private final String schemaContent;
    private final JsonNode schemaNode;

    public JsonSchemaWrapper(String className, String schemaContent, JsonNode schemaNode) {
        this.className = className;
        this.schemaContent = schemaContent;
        this.schemaNode = schemaNode;
    }

    public String getClassName() {
        return className;
    }

    public String getSchemaContent() {
        return schemaContent;
    }

    public JsonNode getSchemaNode() {
        return schemaNode;
    }
}
