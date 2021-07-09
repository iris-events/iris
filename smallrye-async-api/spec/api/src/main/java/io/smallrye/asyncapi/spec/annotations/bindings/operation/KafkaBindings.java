package io.smallrye.asyncapi.spec.annotations.bindings.operation;

import io.smallrye.asyncapi.spec.annotations.media.Schema;

/***
 * This object contains information about the operation representation in Kafka.
 */
public @interface KafkaBindings {
    /**
     * Id of the consumer group.
     *
     * @return groupId property
     */
    Schema groupId() default @Schema();

    /**
     * Id of the consumer inside a consumer group
     *
     * @return clientId property
     */
    Schema clientId() default @Schema();

    /**
     * Version of this binding. If omitted, "latest" MUST be assumed.
     *
     * @return binding version property
     */
    String bindingVersion() default "latest";
}
