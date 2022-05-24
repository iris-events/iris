package id.global.iris.messaging.deployment.validation;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import id.global.iris.messaging.deployment.MessageHandlerValidationException;
import id.global.iris.messaging.deployment.constants.AnnotationInstanceParams;

public class ResourceTypeParamAnnotationValidator implements AnnotationInstanceValidator {

    @Override
    public void validate(AnnotationInstance annotationInstance) {
        final var annotationValue = annotationInstance.value(AnnotationInstanceParams.RESOURCE_TYPE_PARAM);

        final var methodInfo = getMethodInfo(annotationInstance);
        final var declaringClassName = getDeclaringClassName(methodInfo);

        if (annotationValue == null) {
            final var message = "SnapshotMessageHandler annotation in the \"%s\" method of the \"%s\" class requires parameter \"%s\"."
                    .formatted(methodInfo.name(), declaringClassName, AnnotationInstanceParams.RESOURCE_TYPE_PARAM);
            throw new MessageHandlerValidationException(message);
        }

        final var resourceType = annotationValue.asString();
        if (!KEBAB_CASE_PATTERN.matcher(resourceType).matches()) {
            final var message = "SnapshotMessageHandler annotation in the \"%s\" method of the \"%s\" class requires parameter \"%s\" to be formatted in kebab case."
                    .formatted(methodInfo.name(), declaringClassName, AnnotationInstanceParams.RESOURCE_TYPE_PARAM);
            throw new MessageHandlerValidationException(message);
        }
    }

    private MethodInfo getMethodInfo(final AnnotationInstance annotationInstance) {
        return annotationInstance.target().asMethod();
    }

    private DotName getDeclaringClassName(final MethodInfo methodInfo) {
        return methodInfo.declaringClass().name();
    }

}
