package org.iris_events.asyncapi.runtime.scanner.model;

/**
 * Flags to enable/disable certain aspects of the {@link GidOpenApiModule}'s processing.
 * Gid Open Api Option inspired by
 * <a href="https://github.com/victools/jsonschema-generator/tree/main/jsonschema-module-swagger-1.5">Java JSON Schema Generator
 * – Module Swagger (1.5)</a>.
 */
public enum GidOpenApiOption {
    /**
     * Use this option to ignore properties annotated with {@code @SchemaProperty(hidden = true)}.
     */
    IGNORING_HIDDEN_PROPERTIES,
    /**
     * Use this option to apply alternative property names specified via {@code @SchemaProperty(name = "...")}.
     */
    ENABLE_PROPERTY_NAME_OVERRIDES,
    /**
     * Use this option to NOT set the "description" property (based on {@code @SchemaProperty(description = "...")}).
     */
    NO_APIMODEL_DESCRIPTION,
    /**
     * Use this option to NOT set the "title" property (based on {@code @SchemaProperty(value = "...")}).
     */
    NO_APIMODEL_TITLE;
}
