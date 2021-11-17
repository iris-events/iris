package id.global.common.annotations.amqp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
     * Defines allowed roles to use this event handler
     */
    String[] rolesAllowed() default {};
}
