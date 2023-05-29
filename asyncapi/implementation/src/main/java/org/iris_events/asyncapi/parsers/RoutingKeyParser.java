package org.iris_events.asyncapi.parsers;

import java.util.Objects;

import org.iris_events.asyncapi.runtime.util.JandexUtil;
import org.jboss.jandex.AnnotationInstance;

import org.iris_events.annotations.Message;

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
