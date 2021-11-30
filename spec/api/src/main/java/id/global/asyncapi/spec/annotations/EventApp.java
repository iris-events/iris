package id.global.asyncapi.spec.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import id.global.asyncapi.spec.annotations.info.Info;
import id.global.asyncapi.spec.annotations.servers.Server;

@Target({ ElementType.TYPE, ElementType.PACKAGE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface EventApp {
    /**
     * Required: Provides metadata about the API. The metadata MAY be used by tooling as required.
     *
     * @return the metadata about this API
     */
    Info info();

    /**
     * Any additional external documentation for the API
     *
     * @return the external documentation for this API.
     */
    ExternalDocumentation externalDocs() default @ExternalDocumentation;

    /**
     * An array of Server Objects, which provide connectivity information to a target server. If the servers property is not
     * provided, or is an empty
     * array, the default value would be a Server Object with a url value of /.
     *
     * @return the servers of this API
     */
    Server[] servers() default {};
}
