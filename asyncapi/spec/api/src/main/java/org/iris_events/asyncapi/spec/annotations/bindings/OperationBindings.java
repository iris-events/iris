package org.iris_events.asyncapi.spec.annotations.bindings;

import org.iris_events.asyncapi.spec.annotations.bindings.operation.AmqpBindings;
import org.iris_events.asyncapi.spec.annotations.bindings.operation.HttpBindings;
import org.iris_events.asyncapi.spec.annotations.bindings.operation.KafkaBindings;
import org.iris_events.asyncapi.spec.annotations.bindings.operation.MqttBindings;

public @interface OperationBindings {

    /**
     * Protocol-specific information for an HTTP server.
     *
     * @return bindings object for this specific protocol
     */
    HttpBindings http() default @HttpBindings();

    /**
     * Protocol-specific information for an Kafka server.
     *
     * @return bindings object for this specific protocol
     */
    KafkaBindings kafka() default @KafkaBindings();

    /**
     * Protocol-specific information for an AMQP server.
     *
     * @return bindings object for this specific protocol
     */
    AmqpBindings amqp() default @AmqpBindings();

    /**
     * Protocol-specific information for an MQTT server.
     *
     * @return bindings object for this specific protocol
     */
    MqttBindings mqtt() default @MqttBindings();
}
