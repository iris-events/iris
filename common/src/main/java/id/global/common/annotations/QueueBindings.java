package id.global.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotation;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@JacksonAnnotation
public @interface QueueBindings {

    String queueName() default "";

    boolean queueExclusive() default false;

    boolean queueDurable() default true;

    boolean queueAutoDelete() default false;

    String queueVhost() default "/";
}

