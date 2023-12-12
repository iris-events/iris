package org.iris_events.asyncapi.runtime.util;

import static org.iris_events.asyncapi.spec.annotations.enums.SchemaType.ARRAY;
import static org.iris_events.asyncapi.spec.annotations.enums.SchemaType.INTEGER;
import static org.iris_events.asyncapi.spec.annotations.enums.SchemaType.OBJECT;
import static org.iris_events.asyncapi.spec.annotations.enums.SchemaType.STRING;

import java.util.Map;
import java.util.Optional;

import org.iris_events.annotations.Scope;
import org.iris_events.asyncapi.api.Headers;
import org.iris_events.asyncapi.runtime.scanner.model.ChannelInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Schema;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26SchemaImpl;

public class HeaderSchemaBuilder {

    private static final String ROLES_ALLOWED_DESCRIPTION = "Allowed roles for this message. Default is empty";
    private static final String SCOPE_DESCRIPTION = "Message scope. Default is INTERNAL";
    private static final String DEAD_LETTER_DESCRIPTION = "Dead letter queue definition. Default is dead-letter";
    private static final String TTL_DESCRIPTION = "TTL of the message. If set to -1 (default) will use brokers default.";
    private static final String RPC_RESPONSE_DESCRIPTION = "RPC response type property.";

    private final ObjectMapper objectMapper;

    public HeaderSchemaBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AsyncApi26SchemaImpl buildHeaders(ChannelInfo channelInfo, Map<String, Scope> messageScopes) {
        final var headersSchema = new AsyncApi26SchemaImpl();
        headersSchema.setType(OBJECT.toString());

        headersSchema.addProperty(Headers.HEADER_SCOPE, buildScopeSchema(channelInfo, messageScopes));
        headersSchema.addProperty(Headers.HEADER_TTL, buildTtlSchema(channelInfo));
        headersSchema.addProperty(Headers.HEADER_ROLES_ALLOWED, buildRolesAllowedSchema(channelInfo));
        headersSchema.addProperty(Headers.HEADER_DEAD_LETTER, buildDeadLetterSchema(channelInfo));

        Optional.ofNullable(buildRpcResponseTypeSchema(channelInfo)).ifPresent(
                rpcResponseTypeSchema -> headersSchema.addProperty(Headers.RPC_RESPONSE_TYPE, rpcResponseTypeSchema));

        return headersSchema;
    }

    private AsyncApi26Schema buildRolesAllowedSchema(final ChannelInfo channelInfo) {
        final var rolesAllowed = channelInfo.getRolesAllowed();
        final var rolesAllowedSchema = new AsyncApi26SchemaImpl();

        rolesAllowedSchema.setType(ARRAY.toString());
        rolesAllowedSchema.setDescription(ROLES_ALLOWED_DESCRIPTION);

        final var extensionSchemas = objectMapper.createArrayNode();
        rolesAllowed.forEach(extensionSchemas::add);
        rolesAllowedSchema.addExtension("value", extensionSchemas);

        return rolesAllowedSchema;
    }

    private AsyncApi26Schema buildScopeSchema(final ChannelInfo channelInfo, Map<String, Scope> messageScopes) {
        final var scope = getScope(messageScopes, channelInfo.getEventKey());

        final var scopeSchema = new AsyncApi26SchemaImpl();
        scopeSchema.setType(STRING.toString());
        scopeSchema.setDescription(SCOPE_DESCRIPTION);
        scopeSchema.addExtension("value", TextNode.valueOf(scope.name()));

        return scopeSchema;
    }

    private AsyncApi26Schema buildDeadLetterSchema(final ChannelInfo channelInfo) {
        final var deadLetterQueue = channelInfo.getDeadLetterQueue();

        final var deadLetterSchema = new AsyncApi26SchemaImpl();
        deadLetterSchema.setType(STRING.toString());
        deadLetterSchema.setDescription(DEAD_LETTER_DESCRIPTION);
        deadLetterSchema.addExtension("value", TextNode.valueOf(deadLetterQueue));

        return deadLetterSchema;
    }

    private AsyncApi26Schema buildTtlSchema(final ChannelInfo channelInfo) {
        final var ttl = channelInfo.getTtl();

        final var ttlSchema = new AsyncApi26SchemaImpl();
        ttlSchema.setType(INTEGER.toString());
        ttlSchema.setDescription(TTL_DESCRIPTION);
        ttlSchema.addExtension("value", IntNode.valueOf(ttl));

        return ttlSchema;
    }

    private AsyncApi26Schema buildRpcResponseTypeSchema(final ChannelInfo channelInfo) {
        final var rpcResponseType = channelInfo.getRpcResponseType();
        if (rpcResponseType == null) {
            return null;
        }
        final var rpcResponseTypeEventName = rpcResponseType.asClassType().name().toString();
        final var rpcResponseSchema = new AsyncApi26SchemaImpl();
        rpcResponseSchema.setType(STRING.toString());
        rpcResponseSchema.setDescription(RPC_RESPONSE_DESCRIPTION);
        rpcResponseSchema.addExtension("value", TextNode.valueOf(rpcResponseTypeEventName));

        return rpcResponseSchema;
    }

    private Scope getScope(Map<String, Scope> messageScopes, String messageKey) {
        return messageScopes.get(messageKey);
    }
}
