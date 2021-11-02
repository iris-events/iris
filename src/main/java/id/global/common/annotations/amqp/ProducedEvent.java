package id.global.common.annotations.amqp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ProducedEvent {
    /**
     * To which routingKey/routingKey this event is published
     */
    String routingKey() default "";

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
    Scope scope() default Scope.INTERNAL;

    /**
     * Defines allowed roles to produce this event
     */
    String[] rolesAllowed() default {};
}
