package org.iris_events.asyncapi.spec.annotations.bindings.message;

import org.iris_events.asyncapi.spec.annotations.media.Schema;

/**
 * This object contains information about the message representation in Kafka.
 */
public @interface KafkaBindings {

    /**
     * Message key.
     *
     * @return key property
     */
    Schema key() default @Schema();

    /**
     * Version of this binding. If omitted, "latest" MUST be assumed.
     *
     * @return binding version property
     */
    String bindingVersion() default "";
}
