package org.iris_events.asyncapi.runtime.io.schema;

import static org.iris_events.asyncapi.runtime.io.JsonUtil.readNodeArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.iris_events.asyncapi.runtime.io.JsonUtil;
import org.iris_events.asyncapi.runtime.json.IrisObjectMapper;
import org.iris_events.asyncapi.runtime.scanner.model.GidAsyncApi26Schema;
import org.iris_events.asyncapi.runtime.util.JandexUtil;
import org.iris_events.asyncapi.spec.annotations.enums.SchemaType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26SchemaImpl;

//import io.apicurio.datamodels.asyncapi.models.AaiSchema;
//import io.apicurio.datamodels.asyncapi.v2.models.Aai20Schema;

public class SchemaReader {
    public static final String PROP_$REF = "$ref";

    private SchemaReader() {
    }

    /**
     * Reads a {@link AsyncApiSchema} OpenAPI node.
     *
     * @param node json node
     * @return Schema model
     */
    public static GidAsyncApi26Schema readSchema(final JsonNode node) {
        return readSchema(node, false);
    }

    public static GidAsyncApi26Schema readSchema(final JsonNode node, boolean fixRef) {
        if (node == null || !node.isObject()) {
            return null;
        }
        GidAsyncApi26Schema schema = new GidAsyncApi26Schema();

        if (fixRef) {
            schema.set$ref(fixRef(JsonUtil.stringProperty(node, PROP_$REF)));
        } else {
            schema.set$ref(JsonUtil.stringProperty(node, PROP_$REF));
        }
        schema.setFormat(JsonUtil.stringProperty(node, SchemaConstant.PROP_FORMAT));
        schema.setTitle(JsonUtil.stringProperty(node, SchemaConstant.PROP_TITLE));
        schema.setDescription(JsonUtil.stringProperty(node, SchemaConstant.PROP_DESCRIPTION));
        schema.setDefault(node.get(SchemaConstant.PROP_DEFAULT));
        schema.setMultipleOf(JsonUtil.bigDecimalProperty(node, SchemaConstant.PROP_MULTIPLE_OF));
        schema.setMaximum(JsonUtil.bigDecimalProperty(node, SchemaConstant.PROP_MAXIMUM));
        schema.setExclusiveMaximum(JsonUtil.bigDecimalProperty(node, SchemaConstant.PROP_EXCLUSIVE_MAXIMUM));
        schema.setMinimum(JsonUtil.bigDecimalProperty(node, SchemaConstant.PROP_MINIMUM));
        schema.setExclusiveMinimum(JsonUtil.bigDecimalProperty(node, SchemaConstant.PROP_EXCLUSIVE_MINIMUM));
        schema.setMaxLength((JsonUtil.intProperty(node, SchemaConstant.PROP_MAX_LENGTH)));
        schema.setMinLength((JsonUtil.intProperty(node, SchemaConstant.PROP_MIN_LENGTH)));
        schema.setPattern((JsonUtil.stringProperty(node, SchemaConstant.PROP_PATTERN)));
        schema.setMaxItems((JsonUtil.intProperty(node, SchemaConstant.PROP_MAX_ITEMS)));
        schema.setMinItems((JsonUtil.intProperty(node, SchemaConstant.PROP_MIN_ITEMS)));
        schema.setUniqueItems((JsonUtil.booleanProperty(node, SchemaConstant.PROP_UNIQUE_ITEMS).orElse(null)));
        schema.setMaxProperties((JsonUtil.intProperty(node, SchemaConstant.PROP_MAX_PROPERTIES)));
        schema.setMinProperties((JsonUtil.intProperty(node, SchemaConstant.PROP_MIN_PROPERTIES)));
        schema.setRequired(JsonUtil.readStringArray(node.get(SchemaConstant.PROP_REQUIRED)).orElse(null));

        final var enums = readNodeArray(node.get(SchemaConstant.PROP_ENUM));
        if (!enums.isEmpty()) {
            schema.setEnum(enums);
        }

        SchemaType schemaType = readSchemaType(node.get(SchemaConstant.PROP_TYPE));
        schema.setType((schemaType != null) ? schemaType.toString() : null);
        schema.setItems((readSchema(node.get(SchemaConstant.PROP_ITEMS), true)));
        schema.setNot((readSchema(node.get(SchemaConstant.PROP_NOT), true)));

        final var schemaProperties = readSchemas(node.get(SchemaConstant.PROP_PROPERTIES), fixRef).orElse(Map.of());
        schemaProperties.forEach(schema::addProperty);

        //        schema.getProperties().putAll(schemaProperties);
        schema.setReadOnly((JsonUtil.booleanProperty(node, SchemaConstant.PROP_READ_ONLY).orElse(null)));
        schema.setExamples((readNodeArray(node.get(SchemaConstant.PROP_EXAMPLE))));

        final var allOfSchemas = readSchemaArray(node.get(SchemaConstant.PROP_ALL_OF), fixRef).orElse(List.of());
        allOfSchemas.forEach(schema::addAllOf);

        final var oneOfSchemas = readSchemaArray(node.get(SchemaConstant.PROP_ONE_OF), fixRef).orElse(List.of());
        oneOfSchemas.forEach(schema::addOneOf);

        final var anyOfSchemas = readSchemaArray(node.get(SchemaConstant.PROP_ANY_OF), fixRef).orElse(List.of());
        anyOfSchemas.forEach(schema::addAnyOf);

        schema.setNot((readSchema(node.get(SchemaConstant.PROP_NOT))));
        schema.setWriteOnly((JsonUtil.booleanProperty(node, SchemaConstant.PROP_WRITE_ONLY).orElse(null)));
        schema.setDeprecated((JsonUtil.booleanProperty(node, SchemaConstant.PROP_DEPRECATED).orElse(null)));

        // TODO this is confusing, revisit
        if (node.get(SchemaConstant.PROP_ADDITIONAL_PROPERTIES) != null) {
            final var additionalPropertiesObjectNode = IrisObjectMapper.getObjectMapper().createObjectNode();
            final var propertiesMap = readPropertiesMap(node.get(SchemaConstant.PROP_ADDITIONAL_PROPERTIES), fixRef);
            propertiesMap.forEach((key, value) -> {
                additionalPropertiesObjectNode.putIfAbsent(key, TextNode.valueOf(value));
            });
            schema.addExtension("additionalProperties", additionalPropertiesObjectNode);
        }

        schema.existingJavaType = JsonUtil.stringProperty(node, SchemaConstant.PROP_EXISTING_JAVA_TYPE);
        schema.addExtension(SchemaConstant.PROP_EXISTING_JAVA_TYPE,
                TextNode.valueOf(JsonUtil.stringProperty(node, SchemaConstant.PROP_EXISTING_JAVA_TYPE)));

        return schema;
    }

    private static Map<String, String> readPropertiesMap(final JsonNode jsonNode, final boolean fixRef) {
        Map<String, String> props = new HashMap<>();
        jsonNode.fields().forEachRemaining(stringJsonNodeEntry -> {
            final var key = stringJsonNodeEntry.getKey();
            var value = stringJsonNodeEntry.getValue().asText();

            if (key.equals(PROP_$REF)) {
                value = fixRef(value);
            }
            props.put(key, value);
        });
        return props;
    }

    private static String fixRef(String ref) {
        // Sorry, hack for now
        if (ref != null) {
            return ref.replace("/definitions/", "/components/schemas/");
        }
        return null;
    }

    /**
     * Reads a schema type.
     *
     * @param node the json node
     * @return SchemaType enum
     */
    private static SchemaType readSchemaType(final JsonNode node) {
        if (node != null && node.isTextual()) {
            String strval = node.asText();
            return SchemaType.valueOf(strval.toUpperCase());
        }
        return null;
    }

    /**
     * Reads the {@link AsyncApiSchema} OpenAPI nodes.
     *
     * @param node map of schema json nodes
     * @return Map of Schema model
     */

    public static Optional<Map<String, GidAsyncApi26Schema>> readSchemas(final JsonNode node) {
        return readSchemas(node, false);
    }

    public static Optional<Map<String, GidAsyncApi26Schema>> readSchemas(final JsonNode node, boolean fixRef) {
        if (node != null && node.isObject()) {
            Map<String, GidAsyncApi26Schema> models = new LinkedHashMap<>();
            for (Iterator<String> fieldNames = node.fieldNames(); fieldNames.hasNext();) {
                String fieldName = fieldNames.next();
                JsonNode childNode = node.get(fieldName);
                models.put(fieldName, readSchema(childNode, fixRef));
            }
            return Optional.of(models);
        }
        return Optional.empty();
    }

    /**
     * Reads a list of schemas.
     *
     * @param node the json array
     * @return List of Schema models
     */
    private static Optional<List<AsyncApiSchema>> readSchemaArray(final JsonNode node, final boolean fixRef) {
        if (node != null && node.isArray()) {
            List<AsyncApiSchema> rval = new ArrayList<>(node.size());
            ArrayNode arrayNode = (ArrayNode) node;
            for (JsonNode arrayItem : arrayNode) {
                rval.add(readSchema(arrayItem, fixRef));
            }
            return Optional.of(rval);
        }
        return Optional.empty();
    }

    public static AsyncApiSchema readParameterSchema(AnnotationValue parameterSchema) {
        if (parameterSchema != null) {
            AnnotationInstance annotationInstance = parameterSchema.asNested();
            AsyncApiSchema schema = new AsyncApi26SchemaImpl();
            SchemaType schemaType = JandexUtil.enumValue(annotationInstance, SchemaConstant.PROP_TYPE, SchemaType.class);
            if (schemaType != null) {
                schema.setType(schemaType.toString());
            }
            return schema;
        }
        return null;
    }
}
