package id.global.asyncapi.spec.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface MessageHandler {
    // new
    String queue() default Constants.UNASSIGNED;

    /**
     * On which exchange to listen to direct messages
     */
    String exchange() default "";

    /**
     * Override the event class. If not present the first method parameter class type will be used
     */
    Class<?> eventType() default Void.class;
}