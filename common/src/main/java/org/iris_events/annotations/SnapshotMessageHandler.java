package org.iris_events.annotations;

import jakarta.annotation.security.RolesAllowed;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface SnapshotMessageHandler {

    /**
     * Defines the resourceType. It used to bind the consumer of the annotated event class to the correct queue.
     */
    String resourceType();

    /**
     * Defines allowed roles to use this event handler
     */
    RolesAllowed rolesAllowed() default @RolesAllowed({});

    /**
     * Defines how many messages are fetched at once
     *
     * @return number of messages to fetch
     */
    int prefetchCount() default 1;
}
