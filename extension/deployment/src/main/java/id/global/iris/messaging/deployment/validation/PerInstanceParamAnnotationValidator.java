package id.global.iris.messaging.deployment.validation;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import id.global.iris.amqp.parsers.MessageScopeParser;
import id.global.iris.common.annotations.Scope;
import id.global.iris.messaging.deployment.MessageHandlerValidationException;
import id.global.iris.messaging.deployment.constants.AnnotationInstanceParams;
import id.global.iris.messaging.deployment.scanner.ScannerUtils;

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
