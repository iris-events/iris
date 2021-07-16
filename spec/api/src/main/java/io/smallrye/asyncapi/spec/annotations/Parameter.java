package io.smallrye.asyncapi.spec.annotations;

import io.smallrye.asyncapi.spec.annotations.media.Schema;

/**
 * Describes operations available on a single channel
 */
public @interface Parameter {
    /**
     * Optional description of this channel item. <a href="http://spec.commonmark.org/">CommonMark</>
     * syntax can be used for rich text representation.
     *
     * @return channel description
     */
    String description() default "";

    /**
     * Definition of the parameter.
     *
     * @return channels schema
     */
    Schema schema() default @Schema();

    /**
     * <a href="https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#runtimeExpression">Runtime expression</a>
     * that specifies the location of the parameter value. Even when a definition for the target field exists, it MUST NOT be
     * used to validate
     * this parameter but, instead, the schema property MUST be used.
     *
     * @return location property
     */
    String location() default "";

    /**
     * The name of the parameter.
     * <p>
     * The name is REQUIRED when the parameter is defined within
     * {@link io.smallrye.asyncapi.spec.annotations.channels.ChannelItem} or
     * {@link io.smallrye.asyncapi.spec.annotations.components.Components}.
     * The name will be used as the key to add this schema to the 'parameters' map for reuse.
     * </p>
     *
     * @return the name of the schema
     **/
    String name() default "";
}
