package id.global.common.annotations.amqp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotation;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@JacksonAnnotation
public @interface ExchangeBindings {

    String exchangeName() default "";

    String exchangeType() default "direct";

    boolean exchangeDurable() default true;

    boolean exchangeAutoDelete() default false;

    String exchangeVhost() default "/";
}
