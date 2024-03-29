package org.iris_events.asyncapi.parsers;

import java.util.List;
import java.util.Objects;

import org.iris_events.annotations.Message;
import org.iris_events.annotations.MessageHandler;
import org.iris_events.asyncapi.runtime.util.JandexUtil;
import org.jboss.jandex.AnnotationInstance;

public class BindingKeysParser {
    private static final String MESSAGE_HANDLER_BINDING_KEYS_PARAM = "bindingKeys";

    public static List<String> getFromAnnotationClass(MessageHandler messageHandler, Message message) {
        var bindingKeys = messageHandler.bindingKeys();
        if (Objects.nonNull(bindingKeys) && bindingKeys.length > 1) {
            return List.of(bindingKeys);
        }
        return List.of(message.routingKey());
    }

    public static List<String> getFromAnnotationInstance(final AnnotationInstance messageHandlerAnnotation,
            final AnnotationInstance messageAnnotation) {
        return JandexUtil.stringListValue(messageHandlerAnnotation, MESSAGE_HANDLER_BINDING_KEYS_PARAM)
                .orElse(List.of(RoutingKeyParser.getFromAnnotationInstance(messageAnnotation)));
    }

    public static String getFromAnnotationInstanceAsCsv(final AnnotationInstance messageHandlerAnnotation,
            final AnnotationInstance messageAnnotation) {
        return String.join(",", getFromAnnotationInstance(messageHandlerAnnotation, messageAnnotation));
    }
}
