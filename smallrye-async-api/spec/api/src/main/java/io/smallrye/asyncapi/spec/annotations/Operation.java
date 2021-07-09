package io.smallrye.asyncapi.spec.annotations;

import io.smallrye.asyncapi.spec.annotations.bindings.OperationBindings;
import io.smallrye.asyncapi.spec.annotations.components.Message;
import io.smallrye.asyncapi.spec.annotations.components.OperationTrait;
import io.smallrye.asyncapi.spec.annotations.tags.Tag;

/**
 * Describes a publish or a subscribe operation. This provides a place to document how and why messages are sent and received.
 *
 * <p>
 * For example, an operation might describe a chat application use case where a user sends a text message to a group.
 * A publish operation describes messages that are received by the chat application, whereas a subscribe operation
 * describes messages that are sent by the chat application.
 * </p>
 */
public @interface Operation {
    /**
     * Unique string used to identify the operation. The id MUST be unique among all operations described
     * in the API. The operationId value is case-sensitive. Tools and libraries MAY use the operationId to uniquely
     * identify an operation, therefore, it is RECOMMENDED to follow common programming naming conventions.
     *
     * @return operationId property
     */
    String operationId() default "";

    /**
     * Short summary of what the operation is about.
     *
     * @return summary property
     */
    String summary() default "";

    /**
     * A verbose explanation of the operation. <a href="http://spec.commonmark.org/">CommonMark</a> syntax can be
     * used for rich text representation.
     *
     * @return description property
     */
    String description() default "";

    /**
     * List of tags for API documentation control. Tags can be used for logical grouping of operations.
     *
     * @return tags property
     */
    Tag[] tags() default {};

    /**
     * Additional external documentation for this operation.
     *
     * @return externalDocs property
     */
    ExternalDocumentation externalDocs() default @ExternalDocumentation();

    /**
     * A map where the keys describe the name of the protocol and the values describe protocol-specific definitions
     * for the operation.
     *
     * @return bindings property
     */
    OperationBindings bindings() default @OperationBindings();

    /**
     * A list of traits to apply to the operation object. Traits MUST be merged into the operation object using
     * the <a href="https://tools.ietf.org/html/rfc7386">JSON Merge Patch</a> algorithm in the same order they are defined here.
     *
     * @return traits property
     */
    OperationTrait[] traits() default {};

    /**
     * A definition of the message that will be published or received on this channel. {@code oneOf} is allowed here to
     * specify multiple messages, however, a message MUST be valid only against one of the referenced message objects.
     *
     * @return message property
     */
    Message message() default @Message();
}
