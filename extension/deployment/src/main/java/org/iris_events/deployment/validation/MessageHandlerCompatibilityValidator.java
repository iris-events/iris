package org.iris_events.deployment.validation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.iris_events.annotations.ExchangeType;
import org.iris_events.deployment.MessageHandlerValidationException;
import org.iris_events.deployment.builditem.MessageHandlerInfoBuildItem;

public class MessageHandlerCompatibilityValidator {

    public static final String DEFAULT_BINDING_KEYS = "/";

    public static void validate(final List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems) {
        checkForDuplicateMessageHandlers(messageHandlerInfoBuildItems);
    }

    private static void checkForDuplicateMessageHandlers(final List<MessageHandlerInfoBuildItem> messageHandlerInfoBuildItems) {
        final var uniqueHandlerKeys = new HashSet<String>();
        final var bindingKeysPerTopicMessage = new HashMap<String, HashSet<String>>();

        messageHandlerInfoBuildItems.forEach(item -> {
            final var messageClassName = item.getParameterType().name().toString();
            final var bindingKeysString = buildBindingKeysString(item);
            final var handledMessageKey = String.format("%s#%s", messageClassName, bindingKeysString);

            if (item.getExchangeType() == ExchangeType.TOPIC) {
                final var bindingKeys = item.getBindingKeys();
                final var existingBindingKeys = bindingKeysPerTopicMessage.computeIfAbsent(messageClassName,
                        className -> new HashSet<>());

                if (!Collections.disjoint(existingBindingKeys, bindingKeys)) {
                    throw new MessageHandlerValidationException(
                            String.format(
                                    "Duplicate message handler found for the same binding key of message %s and binding keys %s",
                                    messageClassName,
                                    bindingKeysString));
                }
                existingBindingKeys.addAll(bindingKeys);
            }

            if (!uniqueHandlerKeys.add(handledMessageKey)) {
                throw new MessageHandlerValidationException(
                        String.format("Duplicate message handler found for message %s and binding keys %s",
                                messageClassName,
                                bindingKeysString));
            }
        });
    }

    private static String buildBindingKeysString(final MessageHandlerInfoBuildItem item) {
        if (item.getExchangeType() == ExchangeType.FANOUT) {
            return DEFAULT_BINDING_KEYS;
        }
        if (item.getBindingKeys() == null) {
            return DEFAULT_BINDING_KEYS;
        }
        if (item.getBindingKeys().isEmpty()) {
            return DEFAULT_BINDING_KEYS;
        }
        return String.join(", ", item.getBindingKeys().stream().sorted().toList());
    }
}
