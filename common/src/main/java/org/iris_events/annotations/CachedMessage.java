package org.iris_events.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Iris cacheable event. Events annotated with this annotation will be cached by subscription service when part of the
 * subscription flow.
 * This enables the client to receive snapshot value immediately when they subscribe to certain resource.
 */
@Target({ ElementType.TYPE, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface CachedMessage {

    /**
     * Cached value time to live in seconds with default value of 5 minutes.
     */
    int ttl() default 300;
}
