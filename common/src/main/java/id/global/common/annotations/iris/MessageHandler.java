package id.global.common.annotations.iris;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import id.global.common.auth.jwt.Role;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface MessageHandler {

    /**
     * List of bindings keys. These are used to bind the consumer of the annotated event class to the correct queue.
     * If not specified, the binding key should be generated from the annotated class info. In case of a TOPIC exchange this
     * parameter is required.
     * <p>
     * <ul>
     * <li>For messages traversing through DIRECT type exchanges this should be 1:1 with routingKey on the ProducedEvent
     * annotated class. The list should contain only 1 value.</li>
     * <li>For messages traversing through FANOUT type exchanges this parameter does nothing and should not be defined</li>
     * <li>For messages traversing through TOPIC type exchanges this parameter can contain a list of values with wildcards. For
     * more information see @see <a href="https://www.rabbitmq.com/tutorials/tutorial-five-java.html">Rabbitmq Topics Tutorial
     * for Java clients</></li>
     * </ul>
     * </p>
     *
     * @see <a href="https://www.rabbitmq.com/tutorials/tutorial-four-java.html">Rabbitmq Routing Tutorial for Java clients</a>
     */
    String[] bindingKeys() default {};

    /**
     * Whether the queue defined by the annotated event should be durable or not. This parameter should default to true and is
     * not required.
     *
     * @see <a href="https://www.rabbitmq.com/queues.html#durability">Rabbitmq queues durability</a>
     */
    boolean durable() default true;

    /**
     * Whether the queue defined by the annotated event should be marked for auto delete or not. This parameter should default
     * to false and is not required.
     *
     * @see <a href="https://www.rabbitmq.com/queues.html#temporary-queues">Rabbitmq temporary queues</a>
     */
    boolean autoDelete() default false;

    /**
     * Defines allowed roles to use this event handler
     */
    Role[] rolesAllowed() default {};

    /**
     * Defines consumer per service instance, in case there are multiple replicas / pods of same service running
     * setting this flag to true, would create dedicated queue for each service instance.
     * If this is set to true, {@link #autoDelete()} is enforced to prevent leftover queues.
     * 
     * @return true if queue per service instance must be created
     */
    boolean perInstance() default false;

    /**
     * Defines how many messages are fetched at once
     * 
     * @return number of messages to fetch
     */
    int prefetchCount() default 1;
}
