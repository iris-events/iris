package id.global.event.messaging.deployment.validation;

import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.DEAD_LETTER_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.NAME_PARAM;
import static id.global.event.messaging.deployment.constants.AnnotationInstanceParams.ROUTING_KEY_PARAM;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import id.global.amqp.parsers.ExchangeTypeParser;
import id.global.common.annotations.amqp.ExchangeType;
import id.global.event.messaging.deployment.MessageHandlerValidationException;

class ClassAnnotationValidator extends AbstractAnnotationInstanceValidator {

    public ClassAnnotationValidator() {
        super();
    }

    @Override
    public void validate(final AnnotationInstance annotationInstance, final IndexView index) {
        validateParamsAreKebabCase(annotationInstance, index);
    }

    private void validateParamsAreKebabCase(final AnnotationInstance annotationInstance, IndexView index) {
        final var kebabCaseOnlyParams = Stream.of(NAME_PARAM, DEAD_LETTER_PARAM).collect(Collectors.toSet());

        final var exchangeType = getExchangeType(annotationInstance, index);
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

    @Override
    protected MessageHandlerValidationException createNonKebabCaseParamsFoundException(
            final AnnotationInstance annotationInstance, final Set<String> nonKebabCaseParams) {

        final var nonKebabCaseParamsString = String.join(", ", nonKebabCaseParams);
        throw new MessageHandlerValidationException(
                String.format("Parameter(s) \"%s\" of annotation %s on class %s is not formatted in kebab case.",
                        nonKebabCaseParamsString, annotationInstance.name(), getTargetClassName(annotationInstance)));
    }

    @Override
    protected ExchangeType getExchangeType(AnnotationInstance annotationInstance, IndexView index) {
        return ExchangeTypeParser.getFromAnnotationInstance(annotationInstance, index);
    }

    private DotName getTargetClassName(final AnnotationInstance annotationInstance) {
        return annotationInstance.target().asClass().name();
    }
}