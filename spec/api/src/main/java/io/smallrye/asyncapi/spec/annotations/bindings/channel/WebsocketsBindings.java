package io.smallrye.asyncapi.spec.annotations.bindings.channel;

import io.smallrye.asyncapi.spec.annotations.media.Schema;

/**
 * When using WebSockets, the channel represents the connection. Unlike other protocols that support multiple virtual
 * channels (topics, routing keys, etc.) per connection, WebSockets doesn't support virtual channels or, put it another
 * way, there's only one channel and its characteristics are strongly related to the protocol used for the handshake,
 * i.e., HTTP.
 */
public @interface WebsocketsBindings {

    /**
     * HTTP method to use when establishing the connection. Its value MUST be either {@code GET} or {@code POST}.
     *
     * @return binding method property
     */
    String method() default "";

    /**
     * Schema object containing the definitions for each query parameter.
     * This schema MUST be of type {@code object} and have a {@code properties} key.
     *
     * @return query property
     */
    Schema query() default @Schema();

    /**
     * Schema object containing the definitions of the HTTP headers to use when establishing the connection.
     * This schema MUST be of type {@code object} and have a {@code properties} key.
     *
     * @return headers property
     */
    Schema headers() default @Schema();
}
