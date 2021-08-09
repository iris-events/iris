package id.global.asyncapi.spec.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface TopicMessageHandler {

    /**
     * On which exchange to listen to topic messages
     */
    String exchange();

    /**
     * Bindings keys for topic messages. @see <a href="https://www.rabbitmq.com/tutorials/tutorial-five-python.html">Rabbitmq
     * Topics</a>
     */
    String[] bindingKeys();

    /**
     * Override the event class. If not present the first method parameter class type will be used
     */
    Class<?> eventType() default Void.class;
}