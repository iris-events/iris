package id.global.iris.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import id.global.common.auth.jwt.Role;
import id.global.iris.common.constants.Queues;

/**
 * Iris producible event. Events annotated with this annotation should be used as a parameter in an Iris producer send method.
 */
@Target({ ElementType.TYPE, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Message {
    /**
     * Type of exchange this event gets published to. If the exchange doesn't exist it should be created.
     * This parameter is required.
     */
    ExchangeType exchangeType() default ExchangeType.FANOUT;

    /**
     * Defines the exchange to publish this event message to. If not specified, the exchange should be generated from the
     * annotated class info.
     * If the exchange doesn't exist it should be created.
     */
    String name();

    /**
     * Defines the routing of this message on the specified exchange. If not specified, the routing key should be generated from
     * the annotated class info.
     *
     * @see <a href="https://www.rabbitmq.com/tutorials/tutorial-four-java.html">Rabbitmq Routing Tutorial for Java clients</a>
     */
    String routingKey() default "";

    /**
     * Defines allowed roles to produce this event
     */
    Role[] rolesAllowed() default {};

    /**
     * Whether the event is an internal system event or an external communication event
     */
    Scope scope() default Scope.INTERNAL;

    /**
     * Time to live of the event. If -1 rabbitMq default is used.
     */
    int ttl() default -1;

    /**
     * Dead letter queue definition.
     */
    String deadLetter() default Queues.Constants.DEAD_LETTER;

    /**
     * Defines the optional response message type. Applies only to the asyncapi definition. Response class must also be
     * annotated with the {@link Message} annotation
     */
    Class<?> response() default Void.class;

    /**
     * Defines whether message delivery mode is persistent.Persistent messages will be written to disk by the broker as soon as they reach the queue.
     */
    boolean persistent() default false;
}
