package id.global.iris.amqp.parsers;

import java.util.List;
import java.util.Objects;

import org.jboss.jandex.AnnotationInstance;

import id.global.iris.asyncapi.runtime.util.JandexUtil;
import id.global.iris.common.annotations.Message;
import id.global.iris.common.annotations.MessageHandler;

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
