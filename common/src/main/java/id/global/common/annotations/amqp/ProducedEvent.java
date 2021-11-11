package id.global.common.annotations.amqp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Amqp producible event. Events annotated with this annotation should be used as a parameter in an Amqp producer send method.
 */
@Target({ ElementType.TYPE, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ProducedEvent {
    /**
     * Type of exchange this event gets published to. If the exchange doesn't exist it should be created.
     * This parameter is required.
     */
    ExchangeType exchangeType();

    /**
     * Defines the exchange to publish this event message to. If not specified, the exchange should be generated from the
     * annotated class info.
     * If the exchange doesn't exist it should be created.
     */
    String exchange() default "";

    /**
     * Defines the routing of this message on the specified exchange. If not specified, the routing key should be generated from
     * the annotated class info.
     *
     * @see ConsumedEvent#bindingKeys()
     * @see <a href="https://www.rabbitmq.com/tutorials/tutorial-four-java.html">Rabbitmq Routing Tutorial for Java clients</a>
     */
    String routingKey() default "";

    /**
     * Defines allowed roles to produce this event
     */
    String[] rolesAllowed() default {};

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
    boolean autodelete() default false;

    /**
     * Wether the event is an internal system event or an external communication event
     */
    Scope scope() default Scope.INTERNAL;
}
