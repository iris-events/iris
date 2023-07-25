package org.iris_events.asyncapi.runtime.io.schema;

import static org.iris_events.asyncapi.runtime.io.JsonUtil.readObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.iris_events.asyncapi.runtime.io.JsonUtil;
import org.iris_events.asyncapi.runtime.scanner.model.GidAai20Schema;
import org.iris_events.asyncapi.runtime.util.JandexUtil;
import org.iris_events.asyncapi.spec.annotations.enums.SchemaType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.apicurio.datamodels.asyncapi.models.AaiSchema;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Schema;

public class SchemaReader {
    public static final String PROP_$REF = "$ref";

    private SchemaReader() {
    }

    /**
     * Reads a {@link AaiSchema} OpenAPI node.
     *
     * @param node json node
     * @return Schema model
     */
    public static GidAai20Schema readSchema(final JsonNode node) {
        return readSchema(node, false);
    }

    public static GidAai20Schema readSchema(final JsonNode node, boolean fixRef) {
        if (node == null || !node.isObject()) {
            return null;
        }
        GidAai20Schema schema = new GidAai20Schema();

        if (fixRef) {
            schema.$ref = fixRef(JsonUtil.stringProperty(node, PROP_$REF));
        } else {
            schema.$ref = JsonUtil.stringProperty(node, PROP_$REF);
        }
        schema.format = JsonUtil.stringProperty(node, SchemaConstant.PROP_FORMAT);
        schema.title = JsonUtil.stringProperty(node, SchemaConstant.PROP_TITLE);
        schema.description = JsonUtil.stringProperty(node, SchemaConstant.PROP_DESCRIPTION);
        schema.default_ = readObject(node.get(SchemaConstant.PROP_DEFAULT));
        schema.multipleOf = JsonUtil.bigDecimalProperty(node, SchemaConstant.PROP_MULTIPLE_OF);
        schema.maximum = JsonUtil.bigDecimalProperty(node, SchemaConstant.PROP_MAXIMUM);
        schema.exclusiveMaximum = JsonUtil.bigDecimalProperty(node, SchemaConstant.PROP_EXCLUSIVE_MAXIMUM);
        schema.minimum = JsonUtil.bigDecimalProperty(node, SchemaConstant.PROP_MINIMUM);
        schema.exclusiveMinimum = JsonUtil.bigDecimalProperty(node, SchemaConstant.PROP_EXCLUSIVE_MINIMUM);
        schema.maxLength = (JsonUtil.intProperty(node, SchemaConstant.PROP_MAX_LENGTH));
        schema.minLength = (JsonUtil.intProperty(node, SchemaConstant.PROP_MIN_LENGTH));
        schema.pattern = (JsonUtil.stringProperty(node, SchemaConstant.PROP_PATTERN));
        schema.maxItems = (JsonUtil.intProperty(node, SchemaConstant.PROP_MAX_ITEMS));
        schema.minItems = (JsonUtil.intProperty(node, SchemaConstant.PROP_MIN_ITEMS));
        schema.uniqueItems = (JsonUtil.booleanProperty(node, SchemaConstant.PROP_UNIQUE_ITEMS).orElse(null));
        schema.maxProperties = (JsonUtil.intProperty(node, SchemaConstant.PROP_MAX_PROPERTIES));
        schema.minProperties = (JsonUtil.intProperty(node, SchemaConstant.PROP_MIN_PROPERTIES));
        schema.required = JsonUtil.readStringArray(node.get(SchemaConstant.PROP_REQUIRED)).orElse(null);
        schema.enum_ = (JsonUtil.readObjectArray(node.get(SchemaConstant.PROP_ENUM)).orElse(null));
        SchemaType schemaType = readSchemaType(node.get(SchemaConstant.PROP_TYPE));
        schema.type = (schemaType != null) ? schemaType.toString() : null;
        schema.items = (readSchema(node.get(SchemaConstant.PROP_ITEMS), true));
        schema.not = (readSchema(node.get(SchemaConstant.PROP_NOT), true));
        schema.allOf = (readSchemaArray(node.get(SchemaConstant.PROP_ALL_OF), fixRef).orElse(null));
        schema.properties = readSchemas(node.get(SchemaConstant.PROP_PROPERTIES), fixRef).orElse(null);
        schema.readOnly = (JsonUtil.booleanProperty(node, SchemaConstant.PROP_READ_ONLY).orElse(null));
        schema.example = (readObject(node.get(SchemaConstant.PROP_EXAMPLE)));
        schema.oneOf = (readSchemaArray(node.get(SchemaConstant.PROP_ONE_OF), fixRef).orElse(null));
        schema.anyOf = (readSchemaArray(node.get(SchemaConstant.PROP_ANY_OF), fixRef).orElse(null));
        schema.not = (readSchema(node.get(SchemaConstant.PROP_NOT)));
        schema.writeOnly = (JsonUtil.booleanProperty(node, SchemaConstant.PROP_WRITE_ONLY).orElse(null));
        schema.deprecated = (JsonUtil.booleanProperty(node, SchemaConstant.PROP_DEPRECATED).orElse(null));

        if (node.get(SchemaConstant.PROP_ADDITIONAL_PROPERTIES) != null) {
            schema.additionalProperties = readPropertiesMap(node.get(SchemaConstant.PROP_ADDITIONAL_PROPERTIES), fixRef);
        }

        schema.existingJavaType = JsonUtil.stringProperty(node, SchemaConstant.PROP_EXISTING_JAVA_TYPE);

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
     * Reads the {@link AaiSchema} OpenAPI nodes.
     *
     * @param node map of schema json nodes
     * @return Map of Schema model
     */

    public static Optional<Map<String, AaiSchema>> readSchemas(final JsonNode node) {
        return readSchemas(node, false);
    }

    public static Optional<Map<String, AaiSchema>> readSchemas(final JsonNode node, boolean fixRef) {
        if (node != null && node.isObject()) {
            Map<String, AaiSchema> models = new LinkedHashMap<>();
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
    private static Optional<List<AaiSchema>> readSchemaArray(final JsonNode node, final boolean fixRef) {
        if (node != null && node.isArray()) {
            List<AaiSchema> rval = new ArrayList<>(node.size());
            ArrayNode arrayNode = (ArrayNode) node;
            for (JsonNode arrayItem : arrayNode) {
                rval.add(readSchema(arrayItem, fixRef));
            }
            return Optional.of(rval);
        }
        return Optional.empty();
    }

    public static AaiSchema readParameterSchema(AnnotationValue parameterSchema) {
        if (parameterSchema != null) {
            AnnotationInstance annotationInstance = parameterSchema.asNested();
            AaiSchema schema = new Aai20Schema();
            SchemaType schemaType = JandexUtil.enumValue(annotationInstance, SchemaConstant.PROP_TYPE, SchemaType.class);
            if (schemaType != null) {
                schema.type = schemaType.toString();
            }
            return schema;
        }
        return null;
    }
}
