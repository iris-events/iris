package io.smallrye.asyncapi.spec.annotations.info;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation provides metadata about the API, and maps to the Info object in AsyncAPI Specification 2.
 *
 * @see "https://github.com/asyncapi/spec/blob/master/spec/asyncapi.md#infoObject"
 **/
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Info {
    /**
     * The title of the application.
     *
     * @return the application's title
     **/
    String title();

    /**
     * The version of the API definition.
     *
     * @return the application's version
     **/
    String version();

    /**
     * A short description of the application. CommonMark syntax can be used for rich text representation.
     *
     * @return the application's description
     **/
    String description() default "";

    /**
     * A URL to the Terms of Service for the API. Must be in the format of a URL.
     *
     * @return the application's terms of service
     **/
    String termsOfService() default "";

    /**
     * The contact information for the exposed API.
     *
     * @return a contact for the application
     **/
    Contact contact() default @Contact();

    /**
     * The license information for the exposed API.
     *
     * @return the license of the application
     **/
    License license() default @License(name = "");
}
