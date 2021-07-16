package io.smallrye.asyncapi.spec.annotations.bindings.message;

import io.smallrye.asyncapi.spec.annotations.media.Schema;

/**
 * This object contains information about the message representation in HTTP.
 */
public @interface HttpBindings {
    /**
     * Schema object containing the definitions for HTTP-specific headers.
     * This schema MUST be of type {@code object} and have a {@code properties} key.
     *
     * @return headers property
     */
    Schema headers() default @Schema();

    /**
     * Version of this binding. If omitted, "latest" MUST be assumed.
     *
     * @return binding version property
     */
    String bindingVersion() default "";
}
