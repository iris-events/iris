/**
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 * Copyright 2017 SmartBear Software
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smallrye.asyncapi.spec.annotations.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.smallrye.asyncapi.spec.annotations.enums.SecuritySchemeIn;
import io.smallrye.asyncapi.spec.annotations.enums.SecuritySchemeType;

/**
 * Defines a security scheme that can be used by the operations.
 * Supported schemes are HTTP authentication, an API key (either as a header or as a query parameter),
 * OAuth2's common flows (implicit, password, application and access code) as defined in RFC6749, and OpenID Connect Discovery.
 * 
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#security-scheme-object"
 **/
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(SecuritySchemes.class)
@Inherited
public @interface SecurityScheme {
    /**
     * The name of this SecurityScheme. Used as the key to add this security scheme to the 'securitySchemes' map under
     * Components object.
     * <p>
     * It is a REQUIRED property unless this is only a reference to a security scheme instance.
     * </p>
     * 
     * @return the name of this SecurityScheme instance
     **/
    String securitySchemeName() default "";

    /**
     * The type of the security scheme. Valid values are defined by SecuritySchemeType enum. Ignored when empty string.
     * <p>
     * Type is a REQUIRED property unless this is only a reference to a SecuirtyScheme instance.
     * </p>
     * 
     * @return the type of this SecuirtyScheme instance
     **/
    SecuritySchemeType type() default SecuritySchemeType.DEFAULT;

    /**
     * A short description for security scheme.
     * CommonMark syntax can be used for rich text representation.
     * 
     * @return description of this SecurityScheme instance
     **/
    String description() default "";

    /**
     * Applies to and is REQUIRED for SecurityScheme of apiKey type.
     * <p>
     * The name of the header, query or cookie parameter to be used.
     * </p>
     * 
     * @return the name of this apiKey type SecurityScheme instance
     **/
    String apiKeyName() default "";

    /**
     * Applies to and is REQUIRED for SecurityScheme of apiKey type.
     * <p>
     * The location of the API key.
     * Valid values are defined by SecuritySchemeIn enum. Ignored when empty string.
     * </p>
     * 
     * @return the location of the API key
     **/
    SecuritySchemeIn in() default SecuritySchemeIn.DEFAULT;

    /**
     * Applies to and is REQUIRED for SecurityScheme of http type.
     * <p>
     * The name of the HTTP Authorization scheme to be used in the Authorization header as defined in RFC 7235.
     * </p>
     * 
     * @return the name of the HTTP Authorization scheme
     **/
    String scheme() default "";

    /**
     * Applies to http ("bearer") type.
     * <p>
     * A hint to the client to identify how the bearer token is formatted. Bearer tokens are usually generated by an
     * authorization server, so this
     * information is primarily for documentation purposes.
     * </p>
     * 
     * @return the format of the bearer token
     **/
    String bearerFormat() default "";

    /**
     * Applies to and is REQUIRED for SecurityScheme of oauth2 type.
     * <p>
     * An object containing configuration information for the flow types supported.
     * </p>
     * 
     * @return flow types supported by this SecurityScheme instance
     **/
    OAuthFlows flows() default @OAuthFlows;

    /**
     * Applies to and is REQUIRED for SecurityScheme of openIdConnect type.
     * <p>
     * OpenId Connect URL to discover OAuth2 configuration values.
     * This MUST be in the form of a URL.
     * </p>
     * 
     * @return URL where OAuth2 configuration values are stored
     **/
    String openIdConnectUrl() default "";

    /**
     * Reference value to a SecurityScheme object.
     * <p>
     * This property provides a reference to an object defined elsewhere. This property and
     * all other properties are mutually exclusive. If other properties are defined in addition
     * to the ref property then the result is undefined.
     *
     * @return reference to a security scheme
     **/
    String ref() default "";

}
