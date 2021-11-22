package id.global.amqp;

import static id.global.asyncapi.runtime.util.CaseConverter.camelToKebabCase;

import java.util.Objects;

import org.jboss.jandex.AnnotationInstance;

import id.global.asyncapi.runtime.util.JandexUtil;
import id.global.common.annotations.amqp.Message;

public class ExchangeParser {

    private static final String MESSAGE_EXCHANGE_PARAM = "name";

    public static String getFromAnnotationClass(Message messageAnnotation, String messageClassSimpleName) {
        final var exchange = messageAnnotation.name();
        if (!Objects.isNull(exchange) && !exchange.isEmpty()) {
            return exchange;
        }
        return camelToKebabCase(messageClassSimpleName);
    }

    public static String getFromAnnotationInstance(AnnotationInstance messageAnnotation, String messageClassSimpleName) {
        return JandexUtil.optionalStringValue(messageAnnotation, MESSAGE_EXCHANGE_PARAM).
                orElse(camelToKebabCase(messageClassSimpleName));

    }

}
