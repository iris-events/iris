package io.smallrye.asyncapi.runtime.util;

import org.jboss.jandex.DotName;

import id.global.asyncapi.spec.annotations.FanoutMessageHandler;
import id.global.asyncapi.spec.annotations.MessageHandler;
import id.global.asyncapi.spec.annotations.TopicMessageHandler;
import io.smallrye.asyncapi.runtime.scanner.model.ExchangeType;

public class GidAnnotationParser {

    public static ExchangeType getExchangeTypeFromAnnotation(DotName annotationName) {
        if (annotationName.equals(DotName.createSimple(MessageHandler.class.getName())) ||
                annotationName.equals(DotName.createSimple(MessageHandler.class.getSimpleName()))) {
            return ExchangeType.DIRECT;
        }
        if (annotationName.equals(DotName.createSimple(TopicMessageHandler.class.getName())) ||
                annotationName.equals(DotName.createSimple(TopicMessageHandler.class.getSimpleName()))) {
            return ExchangeType.TOPIC;
        }
        if (annotationName.equals(DotName.createSimple(FanoutMessageHandler.class.getName())) ||
                annotationName.equals(DotName.createSimple(FanoutMessageHandler.class.getSimpleName()))) {
            return ExchangeType.FANOUT;
        }
        throw new IllegalArgumentException("Unknown message handler annotation name.");
    }

}
