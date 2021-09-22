package id.global.asyncapi.spec.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import id.global.asyncapi.spec.enums.EventType;
import id.global.asyncapi.spec.enums.ExchangeType;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ProducedEvent {
    /**
     * To which queue/routingKey this event is published
     */
    String queue() default Constants.UNASSIGNED;

    /**
     * To which exchange this event is published
     */
    String exchange() default "";

    /**
     * Type of exchange this event is published to
     */
    ExchangeType exchangeType() default ExchangeType.DIRECT;

    /**
     * Wether the event is an internal system event or an external communication event
     */
    EventType eventType() default EventType.INTERNAL;

    /**
     * Defines allowed roles to produce this event
     */
    String[] rolesAllowed() default {};
}
