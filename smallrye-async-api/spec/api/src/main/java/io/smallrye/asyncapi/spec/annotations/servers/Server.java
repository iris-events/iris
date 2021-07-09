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

package io.smallrye.asyncapi.spec.annotations.servers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.smallrye.asyncapi.spec.annotations.bindings.ServerBindings;
import io.smallrye.asyncapi.spec.annotations.security.SecurityRequirement;

/**
 * This annotation represents a Server used in an operation or used by all operations in an
 * OpenAPI document.
 * <p>
 * When a Server annotation appears on a method the server is added to the corresponding
 * OpenAPI operation servers field.
 * <p>
 * When a Server annotation appears on a type then the server is added to all the operations
 * defined in that type except for those operations which already have one or more servers
 * defined. The server is also added to the servers defined in the root level of the
 * OpenAPI document.
 * <p>
 * This annotation is {@link Repeatable Repeatable}.
 * <p>
 * <b>Note:</b> If both {@link Server Server} and
 * {@link Servers Servers} annotation are specified on the same type,
 * the server definitions will be combined.
 *
 * @see <a href=
 *      "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#server-object">
 *      OpenAPI Specification Server Object</a>
 **/
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Servers.class)
@Inherited
public @interface Server {

    /**
     * The name of the server property.
     * <p>
     * The name is REQUIRED when the server is defined within {@link io.smallrye.asyncapi.spec.annotations.servers.Servers}.
     * The name will be used as the key to add this server to the servers map for reuse.
     * </p>
     *
     * @return the name of the server
     */
    String name() default "";

    /**
     * A URL to the target host. This URL supports Server Variables and may be
     * relative, to indicate that the host location is relative to the location
     * where the OpenAPI definition is being served. Variable substitutions will
     * be made when a variable is named in {brackets}. This is a REQUIRED
     * property.
     *
     * @return URL to the target host
     **/
    String url() default "";

    /**
     * The protocol property of the server.
     * <p>
     * The protocol this URL supports for connection. Supported protocol include, but are not limited to:
     * amqp, amqps, http, https, jms, kafka, kafka-secure, mqtt, secure-mqtt, stomp, stomps, ws, wss, mercure.
     * </p>
     * This is a REQUIRED property;
     *
     * @return protocol property
     */
    String protocol() default "";

    /**
     * The protocol version property of the server.
     * <p>
     * The version of the protocol used for connection. For instance: AMQP 0.9.1, HTTP 2.0, Kafka 1.0.0, etc.
     * </p>
     *
     * @return protocolVersion property
     */
    String protocolVersion() default "";

    /**
     * An optional string describing the host designated by the URL. CommonMark
     * syntax MAY be used for rich text representation.
     *
     * @return description of the host designated by URL
     **/
    String description() default "";

    /**
     * An array of variables used for substitution in the server's URL template.
     *
     * @return array of variables
     **/
    ServerVariable[] variables() default {};

    /**
     * A declaration of which security mechanisms can be used with this server. The list of values includes alternative
     * security requirement objects that can be used. Only one of the security requirement objects need to be satisfied to
     * authorize
     * a connection or operation.
     *
     * @return a list of security requirement objects for this server instance
     */
    SecurityRequirement[] security() default {};

    /**
     * A map where the keys describe the name of the protocol and the values describe protocol-specific definitions for the
     * server.
     *
     * @return bindings property
     */
    ServerBindings bindings() default @ServerBindings();

}
