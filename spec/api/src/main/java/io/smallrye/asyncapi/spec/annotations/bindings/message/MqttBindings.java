package io.smallrye.asyncapi.spec.annotations.bindings.message;

/**
 * This object contains information about the message representation in MQTT.
 */
public @interface MqttBindings {

    /**
     * Version of this binding. If omitted, "latest" MUST be assumed.
     *
     * @return binding version property
     */
    String bindingVersion() default "";
}
