package id.global.event.messaging.deployment.validation;

import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.EXCHANGE_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.ROUTING_KEY_PARAM;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import id.global.common.annotations.amqp.ExchangeType;
import id.global.event.messaging.deployment.MessageHandlerValidationException;

abstract class AbstractAnnotationInstanceValidator {
    static final String KEBAB_CASE_PATTERN = "^([a-z][a-z0-9]*)(-[a-z0-9]+)*$";
    static final String TOPIC_PATTERN = "^([*#]|[a-z0-9-]+)([.]([*#]|[a-z0-9-]+))*$";

    public AbstractAnnotationInstanceValidator() {
    }

    protected abstract MessageHandlerValidationException createNonKebabCaseParamsFoundException(
            final AnnotationInstance annotationInstance, final Set<String> nonKebabCaseParams);

    protected abstract ExchangeType getExchangeType(AnnotationInstance annotationInstance);

    public void validate(final AnnotationInstance annotationInstance) {
        validateParamsAreKebabCase(annotationInstance);
    }

    private void validateParamsAreKebabCase(final AnnotationInstance annotationInstance) {
        final var kebabCaseOnlyParams = new HashSet<String>();
        kebabCaseOnlyParams.add(EXCHANGE_PARAM);

        final var exchangeType = getExchangeType(annotationInstance);
        if (exchangeType != ExchangeType.TOPIC) {
            kebabCaseOnlyParams.add(ROUTING_KEY_PARAM);
        }
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
}
