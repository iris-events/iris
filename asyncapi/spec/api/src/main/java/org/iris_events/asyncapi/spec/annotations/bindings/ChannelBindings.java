package org.iris_events.asyncapi.spec.annotations.bindings;

import org.iris_events.asyncapi.spec.annotations.bindings.channel.AmqpBindings;
import org.iris_events.asyncapi.spec.annotations.bindings.channel.WebsocketsBindings;

/**
 * Map describing protocol-specific definitions for a server.
 */
public @interface ChannelBindings {
    /**
     * Protocol-specific information for an WebSockets server.
     *
     * @return bindings object for this specific protocol
     */
    WebsocketsBindings ws() default @WebsocketsBindings();

    /**
     * Protocol-specific information for an AMQP server.
     *
     * @return bindings object for this specific protocol
     */
    AmqpBindings amqp() default @AmqpBindings();
}
