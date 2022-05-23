package id.global.iris.plugin.model.generator.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.regex.Pattern;

public class ExistingJavaTypeProcessor {
    private static final Pattern CLASS_NAME_IN_JAVA_TYPE = Pattern.compile("^.*\\.(?:([a-zA-Z]*)(?!\\.))$");
    private static final String REPLACEMENT_PATTERN_TEMPLATE = "((?:[.$a-zA-Z]*)%s)";
    private final ObjectMapper objectMapper;

    public ExistingJavaTypeProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String fixExistingType(final String schemaContent) {
        if (!schemaContent.contains("existingJavaType")) {
            return schemaContent;
        }

        ObjectNode schema = null;
        try {
            schema = (ObjectNode) objectMapper.readTree(schemaContent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        if (!schema.hasNonNull("properties")) {
            return schemaContent;
        }
        var props = (ObjectNode) schema.get("properties");
        String fixedSchema = schemaContent;
        for (var type : props) {
            if (!(type.hasNonNull("existingJavaType") && type.hasNonNull("additionalProperties"))) {
                continue;
            }
            var additional = type.get("additionalProperties");
            if (!additional.hasNonNull("javaType")) {
                continue;
            }
            var existingJavaType = type.get("existingJavaType").asText();
            var javaType = type.get("additionalProperties").get("javaType").asText();
            final var classNameMatcher = CLASS_NAME_IN_JAVA_TYPE.matcher(javaType);

            if (!classNameMatcher.matches()) {
                continue;
            }

            final var className = classNameMatcher.group(1);
            Pattern replacePattern = getReplacementPattern(className);
            final var newJavaType = existingJavaType.replaceAll(replacePattern.pattern(), javaType);
            fixedSchema = schemaContent.replace(existingJavaType, newJavaType);
        }

        return fixedSchema;
    }

    private Pattern getReplacementPattern(final String className) {
        return Pattern.compile(String.format(REPLACEMENT_PATTERN_TEMPLATE, className), Pattern.DOTALL);
    }

}
