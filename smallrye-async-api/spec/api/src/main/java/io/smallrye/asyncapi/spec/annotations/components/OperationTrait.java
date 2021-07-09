package io.smallrye.asyncapi.spec.annotations.components;

import io.smallrye.asyncapi.spec.annotations.ExternalDocumentation;
import io.smallrye.asyncapi.spec.annotations.bindings.OperationBindings;
import io.smallrye.asyncapi.spec.annotations.tags.Tag;

/**
 * Describes a trait that MAY be applied to an
 * <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#operationObject">Operation Object</a>.
 * This object MAY contain any property from the
 * <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#operationObject">
 * Operation Object</a>,
 * except message and traits.
 * <p>
 * If you're looking to apply traits to a message, see the
 * <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#messageTraitObject">Message Trait Object</a>.
 *
 * @see io.smallrye.asyncapi.spec.models.components.MessageTrait
 */
public @interface OperationTrait {
    /**
     * A unique string used to identify the operation. The id MUST be unique among all operations described in
     * the API. The operationId value is case-sensitive. Tools and libraries MAY use the operationId to uniquely identify
     * an operation, therefore, it is RECOMMENDED to follow common programming naming conventions.
     *
     * @return operationId property
     */
    String operationId() default "";

    /**
     * A short summary of what the operation is about.
     *
     * @return summary property
     */
    String summary() default "";

    /**
     * A verbose explanation of the operation. <a href="http://spec.commonmark.org/">CommonMark syntax</a>
     * can be used for rich text representation.
     *
     * @return description property
     */
    String description() default "";

    /**
     * A list of tags for API documentation control. Tags can be used for logical grouping of operations.
     *
     * @return tags property
     */
    Tag tags() default @Tag();

    /**
     * Additional external documentation for this operation.
     *
     * @return externalDocs property
     **/
    ExternalDocumentation externalDocs() default @ExternalDocumentation();

    /**
     * A map where the keys describe the name of the protocol and the values describe protocol-specific definitions
     * for the operation.
     *
     * @return bindings property
     */
    OperationBindings bindings() default @OperationBindings();
}
