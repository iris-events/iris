package id.global.event.messaging.deployment.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.IndexView;

public class AnnotationInstanceValidator {

    private final Map<AnnotationTarget.Kind, AbstractAnnotationInstanceValidator> validatorsForKind = new HashMap<>();
    private final IndexView index;

    public AnnotationInstanceValidator(final IndexView index) {
        this.index = index;
        final var classAnnotationValidator = new ClassAnnotationValidator();
        final var methodAnnotationValidator = new MethodAnnotationValidator(index, classAnnotationValidator);
        validatorsForKind.put(AnnotationTarget.Kind.METHOD, methodAnnotationValidator);
        validatorsForKind.put(AnnotationTarget.Kind.CLASS, classAnnotationValidator);
    }

    public void validate(final AnnotationInstance annotationInstance) {
        final var optionalValidator = findValidator(annotationInstance.target());
        final var validator = optionalValidator.orElseThrow(
                () -> new IllegalArgumentException(
                        "Annotation validator not found. Unsupported annotation target kind: " + annotationInstance.target()
                                .kind()));
        validator.validate(annotationInstance, index);
    }

    private Optional<AbstractAnnotationInstanceValidator> findValidator(final AnnotationTarget target) {
        return Optional.ofNullable(validatorsForKind.get(target.kind()));
    }
}
