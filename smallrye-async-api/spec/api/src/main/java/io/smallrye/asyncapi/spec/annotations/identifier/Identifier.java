package io.smallrye.asyncapi.spec.annotations.identifier;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Identifier {
    /**
     * The id of the application.
     *
     * @return the application's id
     */
    String id();
}
