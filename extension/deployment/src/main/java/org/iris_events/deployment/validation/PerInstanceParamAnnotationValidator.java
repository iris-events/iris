package org.iris_events.deployment.validation;

import org.iris_events.annotations.Scope;
import org.iris_events.asyncapi.parsers.MessageScopeParser;
import org.iris_events.deployment.MessageHandlerValidationException;
import org.iris_events.deployment.constants.AnnotationInstanceParams;
import org.iris_events.deployment.scanner.ScannerUtils;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

public class PerInstanceParamAnnotationValidator implements AnnotationInstanceValidator {

    private final IndexView index;

    public PerInstanceParamAnnotationValidator(IndexView index) {
        this.index = index;
    }

    @Override
    public void validate(AnnotationInstance annotationInstance) {
        final var annotationValue = annotationInstance.value(AnnotationInstanceParams.PER_INSTANCE_PARAM);
        if (annotationValue == null) {
            return;
        }

        final var scope = getMessageScope(annotationInstance);
        if (!scope.equals(Scope.FRONTEND)) {
            return;
        }

        final var perInstance = annotationValue.asBoolean();
        if (!perInstance) {
            return;
        }
        final var methodInfo = getMethodInfo(annotationInstance);
        final var declaringClassName = getDeclaringClassName(methodInfo);
        final var message = "%s = %s in the SnapshotMessageHandler annotation in the \"%s\" method of the \"%s\" class is not supported for the \"%s\" message scope."
                .formatted(AnnotationInstanceParams.PER_INSTANCE_PARAM, perInstance, methodInfo.name(), declaringClassName,
                        scope);
        throw new MessageHandlerValidationException(message);
    }

    private Scope getMessageScope(final AnnotationInstance annotationInstance) {
        final var methodInfo = getMethodInfo(annotationInstance);
        final var messageAnnotation = ScannerUtils.getMessageAnnotation(methodInfo, this.index);

        return MessageScopeParser.getFromAnnotationInstance(messageAnnotation, this.index);
    }

    private MethodInfo getMethodInfo(final AnnotationInstance annotationInstance) {
        return annotationInstance.target().asMethod();
    }

    private DotName getDeclaringClassName(final MethodInfo methodInfo) {
        return methodInfo.declaringClass().name();
    }

}
