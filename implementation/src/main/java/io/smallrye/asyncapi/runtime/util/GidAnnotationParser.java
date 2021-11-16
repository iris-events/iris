package io.smallrye.asyncapi.runtime.util;

import java.util.List;
import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.Scope;
import io.smallrye.asyncapi.runtime.scanner.FilteredIndexView;

public class GidAnnotationParser {
    private static final String METHOD_NAME_ROUTING_KEY = "routingKey";
    private static final String METHOD_NAME_BINDING_KEYS = "bindingKeys";
    private static final String METHOD_NAME_EXCHANGE = "exchange";
    private static final String METHOD_NAME_EXCHANGE_TYPE = "exchangeType";
    private static final String METHOD_NAME_SCOPE = "scope";
    private static final String METHOD_NAME_ROLES_ALLOWED = "rolesAllowed";
    private static final String METHOD_NAME_DURABLE = "durable";
    private static final String METHOD_NAME_AUTODELETE = "autodelete";

    private static final ExchangeType DEFAULT_EXCHANGE_TYPE = ExchangeType.DIRECT;
    private static final Scope DEFAULT_SCOPE = Scope.INTERNAL;
    private static final String[] DEFAULT_ROLES_ALLOWED = new String[0];

    public static String getRoutingKey(AnnotationInstance annotation, final String eventClassSimpleName) {
        return JandexUtil.optionalStringValue(annotation, METHOD_NAME_ROUTING_KEY)
                .orElse(camelToKebabCase(eventClassSimpleName));
    }

    public static String getBindingKeysCsv(AnnotationInstance annotation, final String eventClassSimpleName) {
        return String.join(",", JandexUtil.stringListValue(annotation, METHOD_NAME_BINDING_KEYS)
                .orElse(List.of(camelToKebabCase(eventClassSimpleName))));
    }

    public static String getExchange(AnnotationInstance annotation, String defaultExchange,
            ExchangeType exchangeType) {
        return JandexUtil.optionalStringValue(annotation, METHOD_NAME_EXCHANGE).
                orElse(generateDefaultExchangeName(defaultExchange, exchangeType));
    }

    public static ExchangeType getExchangeType(AnnotationInstance annotation) {
        return JandexUtil.optionalStringValue(annotation, METHOD_NAME_EXCHANGE_TYPE)
                .map(ExchangeType::fromType)
                .orElse(DEFAULT_EXCHANGE_TYPE);
    }

    public static Scope getEventScope(AnnotationInstance annotation) {
        return JandexUtil.optionalStringValue(annotation, METHOD_NAME_SCOPE)
                .map(Scope::valueOf)
                .orElse(DEFAULT_SCOPE);
    }

    public static boolean getEventDurable(AnnotationInstance annotation,
            FilteredIndexView index) {
        return annotation.valueWithDefault(index, METHOD_NAME_DURABLE).asBoolean();
    }

    public static boolean getEventAutodelete(AnnotationInstance annotation, FilteredIndexView index) {
        return annotation.valueWithDefault(index, METHOD_NAME_AUTODELETE).asBoolean();
    }

    public static String[] getRolesAllowed(AnnotationInstance annotation) {
        return Optional.ofNullable(annotation.value(METHOD_NAME_ROLES_ALLOWED))
                .map(AnnotationValue::asStringArray)
                .orElse(DEFAULT_ROLES_ALLOWED);
    }

    public static String camelToKebabCase(final String str) {
        return str.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
    }

    private static String generateDefaultExchangeName(String defaultExchange, ExchangeType exchangeType) {
        return camelToKebabCase(defaultExchange) + "-" + exchangeType.getType();
    }
}
