package id.global.common.annotations.amqp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Amqp consumable event. Events annotated with this annotation should be used as a parameter in a {@link MessageHandler} annotated method.
 */
@Target({ ElementType.TYPE, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ConsumedEvent {
    /**
     * Type of exchange from which this event gets consumed. If the exchange doesn't exist it should be created.
     * This parameter is required.
     */
    ExchangeType exchangeType();

    /**
     * On what exchange to listen to for event messages. If not specified, exchange should be generated from the annotated class info.
     * If the exchange doesn't exist it should be created.
     */
    String exchange() default "";

    /**
     * List of bindings keys. These are used to bind the consumer of the annotated event class to the correct queue.
     * If not specified, the binding key should be generated from the annotated class info. In case of a TOPIC exchange this parameter should be required.
     * <p>
     *      <ul>
     *          <li>For messages traversing through DIRECT type exchanges this should be 1:1 with routingKey on the ProducedEvent annotated class. The list should contain only 1 value.</li>
     *          <li>For messages traversing through FANOUT type exchanges this parameter does nothing and should not be defined</li>
     *          <li>For messages traversing through TOPIC type exchanges this parameter can contain a list of values with wildcards. For more information see @see <a href="https://www.rabbitmq.com/tutorials/tutorial-five-java.html">Rabbitmq Topics Tutorial for Java clients</></li>
     *     </ul>
     * </p>
     *
     * @see ProducedEvent#routingKey()
     * @see <a href="https://www.rabbitmq.com/tutorials/tutorial-four-java.html">Rabbitmq Routing Tutorial for Java clients</a>
     */
    String[] bindingKeys() default {};

    /**
     * Whether the queue defined by the annotated event should be durable or not. This parameter should default to true and is not required.
     *
     * @see <a href="https://www.rabbitmq.com/queues.html#durability">Rabbitmq queues durability</a>
     */
    boolean durable() default true;

    /**
     * Whether the queue defined by the annotated event should be marked for auto delete or not. This parameter should default to false and is not required.
     *
     * @see <a href="https://www.rabbitmq.com/queues.html#temporary-queues">Rabbitmq temporary queues</a>
     */
    boolean autodelete() default false;

    /**
     * Whether the event is intended for internal system communication or for external communication i.e. an event to a api-gateway that gets passed to a frontend client via websocket or rest response.
     * This parameter should default to INTERNAL and is not required.
     */
    Scope scope() default Scope.INTERNAL;
}
