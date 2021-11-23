package id.global.amqp.parsers;

import java.util.Objects;

import org.jboss.jandex.AnnotationInstance;

import id.global.asyncapi.runtime.util.JandexUtil;
import id.global.common.annotations.amqp.Message;

public class RoutingKeyParser {
    private static final String MESSAGE_ROUTING_KEY_PARAM = "routingKey";

    public static String getFromAnnotationClass(final Message messageAnnotation) {
        final var routingKey = messageAnnotation.routingKey();
        if (Objects.nonNull(routingKey) && !routingKey.isBlank()) {
            return routingKey;
        }
        return ExchangeParser.getFromAnnotationClass(messageAnnotation);
    }

    public static String getFromAnnotationInstance(final AnnotationInstance messageAnnotation) {
        return JandexUtil.optionalStringValue(messageAnnotation, MESSAGE_ROUTING_KEY_PARAM)
                .orElse(ExchangeParser.getFromAnnotationInstance(messageAnnotation));
    }
}
