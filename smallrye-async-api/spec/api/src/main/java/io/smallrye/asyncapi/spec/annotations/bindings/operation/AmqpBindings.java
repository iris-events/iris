package io.smallrye.asyncapi.spec.annotations.bindings.operation;

/**
 * This object contains information about the operation representation in AMQP.
 */
public @interface AmqpBindings {
    /**
     * TTL (Time-To-Live) for the message. It Must be greater than or equal to zero.
     *
     * @return expiration property
     */
    int expiration() default 0;

    /**
     * User identificator who has sent the message.
     *
     * @return userId property
     */
    String userId() default "";

    /**
     * Routing keys the message should be routed to at the time of publishing.
     *
     * @return cc property
     */
    String[] cc() default {};

    /**
     * Priority of the message
     *
     * @return priority property
     */
    int priority() default 0;

    /**
     * Delivery mode of the message. Its value MUST be either 1 (transient) or 2 (persistent).
     *
     * @return deliveryMode property
     */
    int deliveryMode() default 0;

    /**
     * Whether the message is mandatory or not.
     *
     * @return mandatory property
     */
    boolean mandatory() default false;

    /**
     * Property similar to {@link io.smallrye.asyncapi.spec.models.bindings.operation.OperationAmqpBindings#getCc}
     * but consumers will not receive this information.
     *
     * @return bcc property
     */
    String[] bcc() default {};

    /**
     * Name of the queue where the consumer should send the response.
     *
     * @return replyTo property
     */
    String replyTo() default "";

    /**
     * Whether the message should include a timestamp or not.
     *
     * @return timestamp property
     */
    boolean timestamp() default false;

    /**
     * Whether the consumer should ack the message or not.
     *
     * @return ack property
     */
    boolean ack() default false;

    /**
     * Version of this binding. If omitted, "latest" MUST be assumed.
     *
     * @return binding version property
     */
    String bindingVersion() default "latest";
}
