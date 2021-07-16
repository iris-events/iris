package io.smallrye.asyncapi.spec.annotations.channels;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This object represents an array of channel items that can be specified for the operation or at definition level.
 **/
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ChannelItems {

    /**
     * An array of ChannelItem annotations that can be specified for the operation or at definition level.
     *
     * @return the array of the ChannelItem annotations
     */
    ChannelItem[] value() default {};
}
