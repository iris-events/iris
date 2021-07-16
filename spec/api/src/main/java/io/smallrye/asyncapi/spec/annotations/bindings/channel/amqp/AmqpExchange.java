package io.smallrye.asyncapi.spec.annotations.bindings.channel.amqp;

/**
 * Defines the exchange properties when {@code is}={@code routingKey}
 */
public @interface AmqpExchange {

    /**
     * Name of the exchange. It MUST NOT exceed 255 characters.
     *
     * @return the name of the exchange
     */
    String name() default "";

    /**
     * Type of the exchange. Can be either {@code topic}, {@code direct}, {@code fanout}, {@code default} or {@code headers}.
     *
     * @return the type of the exchange
     */
    String type() default "";

    /**
     * True or false whether the exchange should survive broker restarts or not.
     *
     * @return exchange durable property
     */
    boolean durable() default false;

    /**
     * True or false whether the exchange should be deleted when the last queue is unbound from it.
     *
     * @return exchange autoDelete property
     */
    boolean autoDelete() default false;

    /**
     * Virtual host of the exchange. Defaults to {@code /}.
     *
     * @return virtual host property
     */
    String vhost() default "";
}
