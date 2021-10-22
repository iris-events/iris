package io.smallrye.asyncapi.runtime.util;

import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import id.global.asyncapi.spec.enums.ExchangeType;
import id.global.asyncapi.spec.enums.Scope;

public class GidAnnotationParser {
    private static final String METHOD_NAME_QUEUE = "queue";
    private static final String METHOD_NAME_EXCHANGE = "exchange";
    private static final String METHOD_NAME_EXCHANGE_TYPE = "exchangeType";
    private static final String METHOD_NAME_SCOPE = "scope";
    private static final String METHOD_NAME_ROLES_ALLOWED = "rolesAllowed";

    private static final String DEFAULT_EXCHANGE = "";
    private static final ExchangeType DEFAULT_EXCHANGE_TYPE = ExchangeType.DIRECT;
    private static final Scope DEFAULT_SCOPE = Scope.INTERNAL;
    private static final String[] DEFAULT_ROLES_ALLOWED = new String[0];

    public static String getQueue(AnnotationInstance annotation, final String eventClassSimpleName) {
        return JandexUtil.optionalStringValue(annotation, METHOD_NAME_QUEUE)
                .orElse(camelToKebabCase(eventClassSimpleName));
    }

    public static String getExchange(AnnotationInstance annotation) {
        return JandexUtil.optionalStringValue(annotation, METHOD_NAME_EXCHANGE).
                orElse(DEFAULT_EXCHANGE);
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

    public static String[] getRolesAllowed(AnnotationInstance annotation) {
        return Optional.ofNullable(annotation.value(METHOD_NAME_ROLES_ALLOWED))
                .map(AnnotationValue::asStringArray)
                .orElse(DEFAULT_ROLES_ALLOWED);
    }

    public static String camelToKebabCase(final String str) {
        return str.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
    }
}
