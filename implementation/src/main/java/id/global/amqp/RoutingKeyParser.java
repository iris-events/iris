package id.global.amqp;

import static id.global.asyncapi.runtime.util.GidAnnotationParser.camelToKebabCase;

import java.util.Objects;

import org.jboss.jandex.AnnotationInstance;

import id.global.asyncapi.runtime.util.JandexUtil;
import id.global.common.annotations.amqp.Message;

public class RoutingKeyParser {
    private static final String MESSAGE_ROUTING_KEY_PARAM = "routingKey";

    public static String getFromAnnotationClass(final Message messageAnnotation, final String messageClassSimpleName) {
        final var routingKey = messageAnnotation.routingKey();
        if (!Objects.isNull(routingKey) && !routingKey.isBlank()) {
            return routingKey;
        }
        return camelToKebabCase(messageClassSimpleName);
    }

    public static String getFromAnnotationInstance(final AnnotationInstance messageAnnotation,
            final String messageClassSimpleName) {
        return JandexUtil.optionalStringValue(messageAnnotation, MESSAGE_ROUTING_KEY_PARAM)
                .orElse(camelToKebabCase(messageClassSimpleName));
    }
}
