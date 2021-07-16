package io.smallrye.asyncapi.spec.annotations.components;

import io.smallrye.asyncapi.spec.annotations.ExternalDocumentation;
import io.smallrye.asyncapi.spec.annotations.bindings.MessageBindings;
import io.smallrye.asyncapi.spec.annotations.media.ExampleObject;
import io.smallrye.asyncapi.spec.annotations.media.Schema;
import io.smallrye.asyncapi.spec.annotations.tags.Tags;

/**
 * Describes a trait that MAY be applied to a
 * <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#messageObject">
 * Message Object</a>.
 * This object MAY contain any property from the
 * <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#messageObject">
 * Message Object</a>,
 * except payload and traits.
 * <p>
 * If you're looking to apply traits to an operation, see the
 * <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#operationTraitObject">Operation Trait Object</a>.
 */
public @interface MessageTrait {

    /**
     * Schema definition of the application headers. Schema MUST be of type "object". It MUST NOT define the protocol headers.
     *
     * @return headers property
     */
    Schema headers() default @Schema();

    /**
     * Definition of the correlation ID used for message tracing or matching.
     *
     * @return correlationId property
     */
    CorrelationId correlationId() default @CorrelationId();

    /**
     * A string containing the name of the schema format/language used to define the message payload. If omitted,
     * implementations should parse the payload as a
     * <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#schemaObject">
     * Schema object</a>.
     *
     * @return schemaFormat property
     */
    String schemaFormat() default "";

    /**
     * The content type to use when encoding/decoding a message's payload. The value MUST be a specific media type
     * (e.g. {@code application/json}). When omitted, the value MUST be the one specified on the
     * <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#defaultContentTypeString">defaultContentType</a>
     * field.
     *
     * @return contentType property
     */
    String contentType() default "";

    /**
     * Machine-friendly name for the message.
     *
     * @return name property
     */
    String name() default "";

    /**
     * A human-friendly title for the message.
     *
     * @return title property
     */
    String title() default "";

    /**
     * A short summary of what the message is about.
     *
     * @return summary property
     */
    String summary() default "";

    /**
     * A verbose explanation of the message. <a href="http://spec.commonmark.org/">CommonMark syntax</a> can be used for
     * rich text representation.
     *
     * @return description property
     */
    String description() default "";

    /**
     * A list of tags for API documentation control. Tags can be used for logical grouping of messages.
     *
     * @return tags property
     */
    Tags tags() default @Tags;

    /**
     * Additional external documentation for this message.
     *
     * @return externalDocs property
     */
    ExternalDocumentation externalDocs() default @ExternalDocumentation;

    /**
     * A map where the keys describe the name of the protocol and the values describe protocol-specific definitions for the
     * message.
     *
     * @return Bindings property
     */
    MessageBindings bindings() default @MessageBindings;

    /**
     * An array of key/value pairs where keys MUST be either headers and/or payload. Values MUST contain examples
     * that validate against the
     * <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#messageObjectHeaders">headers</a>
     * or <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#messageObjectPayload">payload</a> fields,
     * respectively.
     *
     * @return examples property
     */
    ExampleObject[] examples() default {};
}
