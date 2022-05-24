package id.global.iris.messaging.deployment.validation;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import id.global.iris.messaging.deployment.scanner.ScannerUtils;

public class MethodParameterTypeAnnotationValidator implements AnnotationInstanceValidator {

    private final IndexView index;
    private final List<AnnotationInstanceValidator> messageAnnotationValidators;

    public MethodParameterTypeAnnotationValidator(IndexView index,
            List<AnnotationInstanceValidator> messageAnnotationValidators) {
        this.index = index;
        this.messageAnnotationValidators = messageAnnotationValidators;
    }

    @Override
    public void validate(AnnotationInstance annotationInstance) {
        final var methodInfo = annotationInstance.target().asMethod();
        final var messageAnnotation = ScannerUtils.getMessageAnnotation(methodInfo, index);
        messageAnnotationValidators.forEach(validator -> validator.validate(messageAnnotation));
    }

}
