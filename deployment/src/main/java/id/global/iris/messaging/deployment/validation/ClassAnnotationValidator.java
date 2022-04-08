package id.global.iris.messaging.deployment.validation;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import id.global.common.annotations.iris.ExchangeType;
import id.global.common.constants.iris.Queues;
import id.global.iris.amqp.parsers.ExchangeTypeParser;
import id.global.iris.asyncapi.runtime.scanner.validator.ReservedAmqpNamesProvider;
import id.global.iris.messaging.deployment.MessageHandlerValidationException;
import id.global.iris.messaging.deployment.constants.AnnotationInstanceParams;

class ClassAnnotationValidator extends AbstractAnnotationInstanceValidator {

    public ClassAnnotationValidator() {
        super();
    }

    @Override
    public void validate(final AnnotationInstance annotationInstance, final IndexView index) {
        validateReservedQueuesExchanges(annotationInstance, index);
        validateParamsAreKebabCase(annotationInstance, index);
    }

    private void validateReservedQueuesExchanges(AnnotationInstance annotationInstance, IndexView index) {
        final var params = Stream.of(AnnotationInstanceParams.NAME_PARAM, AnnotationInstanceParams.ROUTING_KEY_PARAM);

        final var illigalNames = ReservedAmqpNamesProvider.getReservedNames();
        final var paramsWithIllegalNames = params
                .filter(param -> illigalNames.contains(annotationInstance.valueWithDefault(index, param).asString()))
                .toList();

        if (paramsWithIllegalNames.isEmpty()) {
            return;
        }

        throw createUsingReservedNamesException(annotationInstance, paramsWithIllegalNames);
    }

    private void validateParamsAreKebabCase(final AnnotationInstance annotationInstance, IndexView index) {
        final var kebabCaseOnlyParams = Stream.of(
                AnnotationInstanceParams.NAME_PARAM, AnnotationInstanceParams.DEAD_LETTER_PARAM).collect(Collectors.toSet());

        final var exchangeType = getExchangeType(annotationInstance, index);
        if (exchangeType != ExchangeType.TOPIC) {
            kebabCaseOnlyParams.add(AnnotationInstanceParams.ROUTING_KEY_PARAM);
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
            return Arrays.stream(value.asStringArray()).map(val -> val.replaceFirst(Queues.Constants.DEAD_LETTER_PREFIX, ""))
                    .allMatch(val -> pattern.matcher(val).matches());
        }

        return pattern
                .matcher(value.asString().replaceFirst(Queues.Constants.DEAD_LETTER_PREFIX, ""))
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

    private MessageHandlerValidationException createUsingReservedNamesException(AnnotationInstance annotationInstance,
            List<String> usedReservedNamesList) {
        final var usedReservedNames = String.join(", ", usedReservedNamesList);
        throw new MessageHandlerValidationException(
                String.format("Annotation %s on class %s is using reserved names [%s]",
                        annotationInstance.name(), getTargetClassName(annotationInstance), usedReservedNames));
    }

    @Override
    protected ExchangeType getExchangeType(AnnotationInstance annotationInstance, IndexView index) {
        return ExchangeTypeParser.getFromAnnotationInstance(annotationInstance, index);
    }

    private DotName getTargetClassName(final AnnotationInstance annotationInstance) {
        return annotationInstance.target().asClass().name();
    }
}
