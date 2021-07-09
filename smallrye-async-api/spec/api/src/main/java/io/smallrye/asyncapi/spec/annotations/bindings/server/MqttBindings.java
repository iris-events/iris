package io.smallrye.asyncapi.spec.annotations.bindings.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.smallrye.asyncapi.spec.annotations.bindings.server.mqtt.LastWill;

/**
 * This object contains information about the server representation in MQTT.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface MqttBindings {

    /**
     * The client identifier
     *
     * @return clientId property
     */
    String clientId() default "";

    /**
     * Whether to create a persistent connection or not. When {@code false}, the connection will be persistent.
     *
     * @return clientSession property
     */
    boolean clientSession() default false;

    /**
     * Last Will and Testament configuration.
     *
     * @return lastWill property
     */
    LastWill lastWill() default @LastWill();

    /**
     * Interval in seconds of the longest period of time the broker and the client can endure without sending a message.
     *
     * @return keepAlive property
     */
    int keepAlive() default 0;

    /**
     * The version of this binding. If omitted, "latest" MUST be assumed.
     *
     * @return
     */
    String bindingVersion() default "latest";
}
