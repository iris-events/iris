package org.iris_events.deployment.validation;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import org.iris_events.asyncapi.runtime.scanner.validator.ReservedIrisNamesProvider;
import org.iris_events.annotations.ExchangeType;
import org.iris_events.common.constants.Queues;
import org.iris_events.deployment.MessageHandlerValidationException;
import org.iris_events.deployment.constants.AnnotationInstanceParams;
import org.iris_events.asyncapi.parsers.ExchangeTypeParser;

public class MessageAnnotationValidator implements AnnotationInstanceValidator {

    private static final List<String> RESERVED_NAME_EXCLUSIONS = List.of("iris-subscription", "iris-manager");

    private final String serviceName;
    private final IndexView index;

    public MessageAnnotationValidator(String serviceName, IndexView index) {
        this.serviceName = serviceName;
        this.index = index;
    }

    @Override
    public void validate(final AnnotationInstance annotationInstance) {
        validateReservedQueuesExchanges(annotationInstance, index);
        validateParamsAreKebabCase(annotationInstance, index);
        validateDeadLetterParam(annotationInstance, index);
    }

    private void validateDeadLetterParam(AnnotationInstance annotationInstance, IndexView index) {
        final var deadLetterParamValue = annotationInstance.valueWithDefault(index, AnnotationInstanceParams.DEAD_LETTER_PARAM)
                .asString();

        if (!deadLetterParamValue.startsWith(Queues.Constants.DEAD_LETTER_PREFIX)) {
            throw new MessageHandlerValidationException(
                    String.format("Parameter \"%s\" of annotation %s on class %s must start with the prefix \"%s\".",
                            AnnotationInstanceParams.DEAD_LETTER_PARAM, annotationInstance.name(),
                            getTargetClassName(annotationInstance), Queues.Constants.DEAD_LETTER_PREFIX));
        }
    }

    private void validateReservedQueuesExchanges(AnnotationInstance annotationInstance, IndexView index) {
        final var params = Stream.of(AnnotationInstanceParams.NAME_PARAM, AnnotationInstanceParams.ROUTING_KEY_PARAM);

        final var illigalNames = ReservedIrisNamesProvider.getReservedNames();
        final var paramsWithIllegalNames = params
                .filter(param -> illigalNames.contains(annotationInstance.valueWithDefault(index, param).asString()))
                .toList();

        if (paramsWithIllegalNames.isEmpty() || RESERVED_NAME_EXCLUSIONS.contains(serviceName)) {
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
        AnnotationValue value = annotationInstance.value(param);
        if (value.kind().equals(AnnotationValue.Kind.ARRAY)) {
            return Arrays.stream(value.asStringArray()).map(val -> val.replaceFirst(Queues.Constants.DEAD_LETTER_PREFIX, ""))
                    .allMatch(val -> KEBAB_CASE_PATTERN.matcher(val).matches());
        }

        return KEBAB_CASE_PATTERN
                .matcher(value.asString().replaceFirst(Queues.Constants.DEAD_LETTER_PREFIX, ""))
                .matches();
    }

    private MessageHandlerValidationException createNonKebabCaseParamsFoundException(
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

    private ExchangeType getExchangeType(AnnotationInstance annotationInstance, IndexView index) {
        return ExchangeTypeParser.getFromAnnotationInstance(annotationInstance, index);
    }

    private DotName getTargetClassName(final AnnotationInstance annotationInstance) {
        return annotationInstance.target().asClass().name();
    }
}
