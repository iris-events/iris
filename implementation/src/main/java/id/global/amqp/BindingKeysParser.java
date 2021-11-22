package id.global.amqp;

import static id.global.asyncapi.runtime.util.CaseConverter.camelToKebabCase;

import java.util.List;
import java.util.Objects;

import org.jboss.jandex.AnnotationInstance;

import id.global.asyncapi.runtime.util.JandexUtil;
import id.global.common.annotations.amqp.MessageHandler;

public class BindingKeysParser {
    private static final String MESSAGE_HANDLER_BINDING_KEYS_PARAM = "bindingKeys";

    public static String[] getFromAnnotationClass(MessageHandler messageHandler, String messageClassSimpleName) {
        var bindingKeys = messageHandler.bindingKeys();
        if (!Objects.isNull(bindingKeys) && bindingKeys.length > 1) {
            return bindingKeys;
        }
        return List.of(camelToKebabCase(messageClassSimpleName)).toArray(new String[0]);
    }

    public static List<String> getFromAnnotationInstance(final AnnotationInstance annotation,
            final String messageClassSimpleName) {
        return JandexUtil.stringListValue(annotation, MESSAGE_HANDLER_BINDING_KEYS_PARAM)
                .orElse(List.of(camelToKebabCase(messageClassSimpleName)));
    }

    public static String getFromAnnotationInstanceAsCsv(final AnnotationInstance annotation,
            final String messageClassSimpleName) {
        return String.join(",", getFromAnnotationInstance(annotation, messageClassSimpleName));
    }
}
