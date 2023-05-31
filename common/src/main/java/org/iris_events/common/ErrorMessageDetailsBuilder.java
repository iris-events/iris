package org.iris_events.common;

import java.util.HashMap;
import java.util.Map;

import org.iris_events.common.Exchanges;
import org.iris_events.common.MessagingHeaders;

public class ErrorMessageDetailsBuilder {

    public static final String ERROR_ROUTING_KEY_SUFFIX = ".error";

    public record ErrorMessageDetails(
            String exchange,
            String routingKey,
            Map<String, Object> messageHeaders) {
    }

    public static ErrorMessageDetails build(
            final String originalMessageExchange,
            final Map<String, Object> currentMessageHeaders,
            final long currentTimestamp) {

        final var exchange = Exchanges.ERROR.getValue();
        final var routingKey = getRoutingKey(originalMessageExchange);
        final var messageHeaders = buildMessageHeaders(currentMessageHeaders, currentTimestamp);

        return new ErrorMessageDetails(exchange, routingKey, messageHeaders);
    }

    private static Map<String, Object> buildMessageHeaders(final Map<String, Object> currentMessageHeaders,
            final long currentTimestamp) {
        final var messageHeaders = new HashMap<>(currentMessageHeaders);
        messageHeaders.remove(MessagingHeaders.Message.JWT);
        messageHeaders.put(MessagingHeaders.Message.EVENT_TYPE, Exchanges.ERROR.getValue());
        messageHeaders.put(MessagingHeaders.Message.SERVER_TIMESTAMP, currentTimestamp);

        return messageHeaders;
    }

    /**
     * Routing key for error message is always built from original exchange and error suffix regardless of it's initial routing
     * key.
     */
    private static String getRoutingKey(final String originalMessageExchange) {
        return originalMessageExchange + ERROR_ROUTING_KEY_SUFFIX;
    }
}
