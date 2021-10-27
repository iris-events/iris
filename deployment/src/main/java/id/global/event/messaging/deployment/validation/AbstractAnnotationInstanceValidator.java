package id.global.event.messaging.deployment.validation;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;

import id.global.event.messaging.deployment.MessageHandlerValidationException;

public abstract class AbstractAnnotationInstanceValidator {
    static final String KEBAB_CASE_PATTERN = "^([a-z][a-z0-9]*)(-[a-z0-9]+)*$";
    static final String TOPIC_PATTERN = "^([*#]|[a-z0-9-]+)([.]([*#]|[a-z0-9-]+))*$";
    private final ValidationRules validationRules;

    public AbstractAnnotationInstanceValidator(final ValidationRules validationRules) {
        this.validationRules = validationRules;
    }

    protected abstract MessageHandlerValidationException createMissingParamsException(final AnnotationInstance annotationInstance,
            final Set<String> missingParams);

    protected abstract MessageHandlerValidationException createNonKebabCaseParamsFoundException(
            final AnnotationInstance annotationInstance, final Set<String> nonKebabCaseParams);

    public void validate(final AnnotationInstance annotationInstance) {
        validateParamsExist(annotationInstance);
        validateParamsAreKebabCase(annotationInstance);
    }

    private void validateParamsExist(final AnnotationInstance annotationInstance) {
        final var requiredParams = validationRules.requiredParams();
        if (requiredParams == null) {
            return;
        }

        final var missingParams = requiredParams.stream()
                .filter(requiredParam -> annotationInstance.value(requiredParam) == null)
                .collect(Collectors.toSet());

        if (missingParams.isEmpty()) {
            return;
        }

        throw createMissingParamsException(annotationInstance, missingParams);
    }

    private void validateParamsAreKebabCase(final AnnotationInstance annotationInstance) {
        if (validationRules.kebabCaseOnlyParams() == null) {
            return;
        }

        final var nonKebabCaseParams = validationRules.kebabCaseOnlyParams().stream()
                .filter(kebabCaseOnlyParam -> annotationInstance.value(kebabCaseOnlyParam) != null)
                .filter(kebabCaseOnlyParam -> !paramMatchesKebabCase(kebabCaseOnlyParam, annotationInstance))
                .collect(Collectors.toSet());

        if (nonKebabCaseParams.isEmpty()) {
            return;
        }

        throw createNonKebabCaseParamsFoundException(annotationInstance, nonKebabCaseParams);
    }

    private boolean paramMatchesKebabCase(final String param, final AnnotationInstance annotationInstance) {
        final var annotationValue = annotationInstance.value(param);
        return Pattern.compile(KEBAB_CASE_PATTERN)
                .matcher(annotationValue.asString())
                .matches();
    }
}
