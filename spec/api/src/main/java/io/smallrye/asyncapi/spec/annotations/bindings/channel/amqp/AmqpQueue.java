package io.smallrye.asyncapi.spec.annotations.bindings.channel.amqp;

/**
 * Defines the queue properties when {@code is}={@code queue}
 */
public @interface AmqpQueue {

    /**
     * Name of the queue. It MUST NOT exceed 255 characters.
     *
     * @return queue name
     */
    String name() default "";

    /**
     * True or false whether the queue should survive broker restarts or not.
     *
     * @return queue durable property
     */
    boolean durable() default false;

    /**
     * True or falsw whether the queue should be used only by one connection or not.
     *
     * @return queue exclusive property
     */
    boolean exclusive() default false;

    /**
     * True or false whether the queue should be deleted when the last queue is unbound from it.
     *
     * @return queue autoDelete property
     */
    boolean autoDelete() default false;

    /**
     * Virtual host of the queue. Defaults to {@code /}.
     *
     * @return virtual host property
     */
    String vhost() default "/";
}
