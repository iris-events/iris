package id.global.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotation;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@JacksonAnnotation
public @interface EventMetadata {
    String exchange();

    String routingKey();

    String exchangeType();

    String eventType() default "internal";

    String[] rolesAllowed() default "none";
}
