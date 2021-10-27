package id.global.event.messaging.deployment.validation;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import id.global.event.messaging.deployment.MessageHandlerValidationException;
import id.global.event.messaging.deployment.constants.AnnotationInstanceParams;

public class ClassAnnotationValidator extends AbstractAnnotationInstanceValidator {
    private final ValidationRules validationRules;

    public ClassAnnotationValidator(final ValidationRules validationRules) {
        super(validationRules);
        this.validationRules = validationRules;
    }

    @Override
    public void validate(final AnnotationInstance annotationInstance) {
        super.validate(annotationInstance);

        if (validationRules.checkTopicValidity()) {
            validateTopicValidity(annotationInstance);
        }
    }

    @Override
    protected MessageHandlerValidationException createMissingParamsException(final AnnotationInstance annotationInstance,
            final Set<String> missingParams) {

        final var missingParamsString = String.join(", ", missingParams);
        return new MessageHandlerValidationException(
                String.format("Parameter(s) \"%s\" missing in annotation %s on class %s class.",
                        missingParamsString, annotationInstance.name(), getTargetClassName(annotationInstance)));
    }

    @Override
    protected MessageHandlerValidationException createNonKebabCaseParamsFoundException(
            final AnnotationInstance annotationInstance, final Set<String> nonKebabCaseParams) {

        final var nonKebabCaseParamsString = String.join(", ", nonKebabCaseParams);
        throw new MessageHandlerValidationException(
                String.format("Parameter(s) \"%s\" of annotation %s on class %s is not formatted in kebab case.",
                        nonKebabCaseParamsString, annotationInstance.name(), getTargetClassName(annotationInstance)));
    }

    private void validateTopicValidity(final AnnotationInstance annotationInstance) {
        final var annotationValue = annotationInstance.value(AnnotationInstanceParams.BINDING_KEYS_PARAM);
        if (annotationValue == null) {
            return;
        }

        final var pattern = Pattern.compile(TOPIC_PATTERN);
        final var classInfo = annotationInstance.target().asClass();
        final var className = classInfo.toString();

        List<String> bindingKeys = Arrays.asList(annotationValue.asStringArray());

        bindingKeys.stream()
                .filter(bindingKey -> !pattern.matcher(bindingKey).matches())
                .findAny()
                .ifPresent(bindingKey -> {
                    throw new MessageHandlerValidationException(
                            String.format(
                                    "Binding key \"%s\" on  event annotation in class %s does not conform to the TOPIC format. "
                                            + "Can contain only lowercase alphanumerical characters, dots and wildcards (*).",
                                    bindingKey,
                                    className));
                });
    }

    private DotName getTargetClassName(final AnnotationInstance annotationInstance) {
        return annotationInstance.target().asClass().name();
    }
}
