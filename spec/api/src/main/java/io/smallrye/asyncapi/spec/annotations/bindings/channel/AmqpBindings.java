package io.smallrye.asyncapi.spec.annotations.bindings.channel;

import io.smallrye.asyncapi.spec.annotations.bindings.channel.amqp.AmqpExchange;
import io.smallrye.asyncapi.spec.annotations.bindings.channel.amqp.AmqpQueue;

/**
 * This object contains information about the channel representation in AMQP
 */
public @interface AmqpBindings {
    /**
     * Definition of what type the channel is. Can be either {@code queue} or {@code routingKey} (default)
     *
     * @return type of channel
     */
    String is() default "";

    /**
     * When {@code is}={@code routingKey}, object that defines the exchange properties.
     *
     * @return the exchange property
     */
    AmqpExchange exchange() default @AmqpExchange();

    /**
     * When {@code is}={@code queue}, the object that defines the queue properties.
     *
     * @return the queue property
     */
    AmqpQueue queue() default @AmqpQueue();

    /**
     * The version of this binding. If omitted, "latest" MUST be assumed.
     *
     * @return bindingVersion
     */
    String bindingVersion() default "";
}
