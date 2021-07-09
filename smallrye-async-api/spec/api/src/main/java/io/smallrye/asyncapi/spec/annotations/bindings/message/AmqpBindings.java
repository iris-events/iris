package io.smallrye.asyncapi.spec.annotations.bindings.message;

/**
 * This object contains information about the message representation in AMQP.
 */
public @interface AmqpBindings {
    /**
     * MIME encoding for the message content.
     *
     * @return contentEncoding property
     */
    String contentEncoding() default "";

    /**
     * Application-specific message type
     *
     * @return message type property
     */
    String messageType() default "";

    /**
     * Version of this binding. If omitted, "latest" MUST be assumed.
     *
     * @return binding version property
     */
    String bindingBersion() default "";
}
