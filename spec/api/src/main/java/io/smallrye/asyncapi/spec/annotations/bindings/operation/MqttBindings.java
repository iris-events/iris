package io.smallrye.asyncapi.spec.annotations.bindings.operation;

/**
 * This object contains information about the operation representation in MQTT.
 */
public @interface MqttBindings {

    /**
     * Quality of Service (QoS) levels for the message flow between client and server. Its value MUST be
     * either 0 (At most once delivery), 1 (At least once delivery), or 2 (Exactly once delivery).
     *
     * @return qos property
     */
    int qos() default 0;

    /**
     * Whether the broker should retain the message or not.
     *
     * @return retain property
     */
    boolean retain() default false;

    /**
     * Version of this binding. If omitted, "latest" MUST be assumed.
     *
     * @return binding version property
     */
    String bindingVersion() default "latest";
}
