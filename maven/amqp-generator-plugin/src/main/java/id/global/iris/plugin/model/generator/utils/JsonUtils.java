package id.global.iris.plugin.model.generator.utils;

import org.apache.maven.plugin.logging.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
    private final ObjectMapper objectMapper;
    private final Log log;

    public JsonUtils(ObjectMapper objectMapper, Log log) {
        this.objectMapper = objectMapper;
        this.log = log;
    }

    public String getFormattedJson(String content) {
        var node = getJsonNodeFromString(content);
        return node.toPrettyString();
    }

    public JsonNode getJsonNodeFromString(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("Failed to parse json string!", e);
            throw new RuntimeException(e);
        }
    }
}
