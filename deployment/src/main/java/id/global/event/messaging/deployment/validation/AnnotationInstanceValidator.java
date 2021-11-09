package id.global.event.messaging.deployment.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.IndexView;

public class AnnotationInstanceValidator {

    private final Map<AnnotationTarget.Kind, AbstractAnnotationInstanceValidator> validatorsForKind = new HashMap<>();

    public AnnotationInstanceValidator(final IndexView index, final ValidationRules validationRules) {
        final var classAnnotationValidator = new ClassAnnotationValidator(validationRules);
        final var methodAnnotationValidator = new MethodAnnotationValidator(index, validationRules, classAnnotationValidator);
        validatorsForKind.put(AnnotationTarget.Kind.METHOD, methodAnnotationValidator);
        validatorsForKind.put(AnnotationTarget.Kind.CLASS, classAnnotationValidator);
    }

    public void validate(final AnnotationInstance annotationInstance) {
        final var optionalValidator = findValidator(annotationInstance.target());
        final var validator = optionalValidator.orElseThrow(
                () -> new IllegalArgumentException(
                        "Annotation validator not found. Unsupported annotation target kind: " + annotationInstance.target()
                                .kind()));
        validator.validate(annotationInstance);
    }

    private Optional<AbstractAnnotationInstanceValidator> findValidator(final AnnotationTarget target) {
        return Optional.ofNullable(validatorsForKind.get(target.kind()));
    }
}
