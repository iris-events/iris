package io.smallrye.asyncapi.spec.annotations.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.smallrye.asyncapi.spec.annotations.ExternalDocumentation;
import io.smallrye.asyncapi.spec.annotations.bindings.MessageBindings;
import io.smallrye.asyncapi.spec.annotations.media.ExampleObject;
import io.smallrye.asyncapi.spec.annotations.media.Schema;
import io.smallrye.asyncapi.spec.annotations.tags.Tag;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Message {
    /**
     * Machine-friendly name for the message.
     *
     * @return name property
     */
    String name() default "";

    /**
     * Schema definition of the application headers. Schema MUST be of type "object". It MUST NOT define
     * the protocol headers.
     *
     * @return headers property
     */
    Schema headers() default @Schema();

    /**
     * Definition of the message payload. It can be of any type but defaults to
     * <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#schemaObject">Schema object</a>.
     *
     * @return payload property
     */
    // TODO could be any but defaults to Schema?
    Schema payload() default @Schema();

    /**
     * Definition of the correlation ID used for message tracing or matching.
     *
     * @return correlationId property
     */
    CorrelationId correlationId() default @CorrelationId();

    /**
     * A string containing the name of the schema format used to define the message payload. If omitted,
     * implementations should parse the payload as a
     * <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#schemaObject">
     * Schema object</a>.
     * Check out the <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#messageObjectSchemaFormatTable">
     * supported schema formats table</a>
     * for more information. Custom values are allowed but their implementation is OPTIONAL. A custom value MUST NOT refer
     * to one of the schema formats listed in the
     * <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#messageObjectSchemaFormatTable">table</a>.
     *
     * @return schemaFormat property
     */
    String schemaFormat() default "";

    /**
     * Content type to use when encoding/decoding a message's payload. The value MUST be a specific media type
     * (e.g. {@code application/json}). When omitted, the value MUST be the one specified on the
     * <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#defaultContentTypeString">defaultContentType</a>
     * field.
     *
     * @return contentType property
     */
    String contentType() default "";

    /**
     * Human-friendly title for the message.
     *
     * @return title property
     */
    String title() default "";

    /**
     * Short summary of what the message is about.
     *
     * @return summary property
     */
    String summary() default "";

    /**
     * Verbose explanation of the message. <a href="http://spec.commonmark.org/">CommonMark syntax</a> can be used for
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
    Tag[] tags() default {};

    /**
     * Additional external documentation for this message.
     *
     * @return externalDocs property
     */
    ExternalDocumentation externalDocs() default @ExternalDocumentation();

    /**
     * A map where the keys describe the name of the protocol and the values describe protocol-specific definitions for the
     * message.
     *
     * @return Bindings property
     */
    MessageBindings messageBindings() default @MessageBindings();

    /**
     * Array of key/value pairs where keys MUST be either headers and/or payload. Values MUST contain examples
     * that validate against the
     * <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#messageObjectHeaders">headers</a>
     * or <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#messageObjectPayload">payload</a> fields,
     * respectively.
     *
     * @return examples property
     */
    ExampleObject[] examples() default {};

    /**
     * Array of traits to apply to the message object. Traits MUST be merged into the message object using the
     * <a href="https://tools.ietf.org/html/rfc7386">JSON Merge Patch</a> algorithm in the same order they are defined
     * here. The resulting object MUST be a valid
     * <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#messageObject">Message Object</a>.
     *
     * @return traits property
     */
    MessageTrait[] traits() default {};

    /**
     * Reference value to a Message object.
     * <p>
     * This property provides a reference to an object defined elsewhere. This property and
     * all other properties are mutually exclusive. If other properties are defined in addition
     * to the ref property then the result is undefined.
     *
     * @return reference to a callback object definition
     **/
    String ref() default "";
}
