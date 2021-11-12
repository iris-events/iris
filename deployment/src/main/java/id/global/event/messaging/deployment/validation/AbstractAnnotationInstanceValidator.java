package id.global.event.messaging.deployment.validation;

import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.BINDING_KEYS_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EXCHANGE_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EXCHANGE_TYPE_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.ROUTING_KEY;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.MessageHandler;
import id.global.event.messaging.deployment.MessageHandlerValidationException;

abstract class AbstractAnnotationInstanceValidator {
    static final String KEBAB_CASE_PATTERN = "^([a-z][a-z0-9]*)(-[a-z0-9]+)*$";
    static final String TOPIC_PATTERN = "^([*#]|[a-z0-9-]+)([.]([*#]|[a-z0-9-]+))*$";

    public AbstractAnnotationInstanceValidator() {
    }

    protected abstract MessageHandlerValidationException createMissingParamsException(
            final AnnotationInstance annotationInstance,
            final Set<String> missingParams);

    protected abstract MessageHandlerValidationException createNonKebabCaseParamsFoundException(
            final AnnotationInstance annotationInstance, final Set<String> nonKebabCaseParams);

    public void validate(final AnnotationInstance annotationInstance) {
        validateParamsExist(annotationInstance);
        validateParamsAreKebabCase(annotationInstance);
    }

    private void validateParamsExist(final AnnotationInstance annotationInstance) {
        if (isMessageHandlerAnnotation(annotationInstance)) {
            // MessageHandler has no required params
            return;
        }

        ExchangeType exchangeType = getExchangeType(annotationInstance);
        var requiredParams = getRequiredParams(exchangeType);

        final var missingParams = requiredParams.stream()
                .filter(requiredParam -> annotationInstance.value(requiredParam) == null)
                .collect(Collectors.toSet());

        if (missingParams.isEmpty()) {
            return;
        }

        throw createMissingParamsException(annotationInstance, missingParams);
    }

    private boolean isMessageHandlerAnnotation(AnnotationInstance annotationInstance) {
        return annotationInstance.name().equals(DotName.createSimple(MessageHandler.class.getName()));
    }

    private void validateParamsAreKebabCase(final AnnotationInstance annotationInstance) {
        // BindingKeys are not only kebab-case, but depend on the type of exchange, so they are validated separately
        Set<String> kebabCaseOnlyParams = Set.of(EXCHANGE_PARAM, ROUTING_KEY);
        final var nonKebabCaseParams = kebabCaseOnlyParams.stream()
                .filter(kebabCaseOnlyParam -> annotationInstance.value(kebabCaseOnlyParam) != null)
                .filter(kebabCaseOnlyParam -> !paramMatchesKebabCase(kebabCaseOnlyParam, annotationInstance))
                .collect(Collectors.toSet());

        if (nonKebabCaseParams.isEmpty()) {
            return;
        }

        throw createNonKebabCaseParamsFoundException(annotationInstance, nonKebabCaseParams);
    }

    private boolean paramMatchesKebabCase(final String param, final AnnotationInstance annotationInstance) {
        Pattern pattern = Pattern.compile(KEBAB_CASE_PATTERN);
        AnnotationValue value = annotationInstance.value(param);
        if (value.kind().equals(AnnotationValue.Kind.ARRAY)) {
            return Arrays.stream(value.asStringArray()).allMatch(val -> pattern.matcher(val).matches());
        }

        return pattern
                .matcher(value.asString())
                .matches();
    }

    protected ExchangeType getExchangeType(AnnotationInstance annotationInstance) {
        return ExchangeType.fromType(annotationInstance.value(EXCHANGE_TYPE_PARAM).asString());
    }

    protected Set<String> getRequiredParams(ExchangeType exchangeType) {
        final var requiredParams = new HashSet<String>();
        switch (exchangeType) {
            case DIRECT, FANOUT -> requiredParams.addAll(Set.of(EXCHANGE_TYPE_PARAM));
            case TOPIC -> requiredParams.addAll(Set.of(EXCHANGE_TYPE_PARAM, BINDING_KEYS_PARAM));
        }
        return requiredParams;
    }
}
