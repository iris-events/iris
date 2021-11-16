package id.global.event.messaging.deployment.validation;

import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import id.global.common.annotations.amqp.ProducedEvent;
import id.global.event.messaging.deployment.MessageHandlerValidationException;

class MethodAnnotationValidator extends AbstractAnnotationInstanceValidator {

    private final DotName DOT_NAME_PRODUCED_EVENT = DotName.createSimple(ProducedEvent.class.getCanonicalName());

    private final IndexView index;
    private final ValidationRules validationRules;
    private final ClassAnnotationValidator classAnnotationValidator;

    public MethodAnnotationValidator(final IndexView index, final ValidationRules validationRules,
            final ClassAnnotationValidator classAnnotationValidator) {
        super(validationRules);
        this.index = index;
        this.validationRules = validationRules;
        this.classAnnotationValidator = classAnnotationValidator;
    }

    @Override
    public void validate(final AnnotationInstance annotationInstance) {
        super.validate(annotationInstance);

        validateMethodParamCount(annotationInstance);
        validateMethodReturnType(annotationInstance);

        if (!validationRules.allowExternalDependencyParams()) {
            validateMethodParamExternalDependency(annotationInstance);
        }
    }

    @Override
    protected MessageHandlerValidationException createMissingParamsException(final AnnotationInstance annotationInstance,
            final Set<String> missingParams) {

        final var missingParamsString = String.join(", ", missingParams);
        final var methodInfo = getMethodInfo(annotationInstance);
        final var declaringClassName = getDeclaringClassName(methodInfo);
        throw new MessageHandlerValidationException(
                String.format("Parameter(s) \"%s\" missing on MessageHandler annotation on %s::%s",
                        missingParamsString, declaringClassName, methodInfo.name()));
    }

    @Override
    protected MessageHandlerValidationException createNonKebabCaseParamsFoundException(
            final AnnotationInstance annotationInstance, final Set<String> nonKebabCaseParams) {

        final var nonKebabCaseParamsString = String.join(", ", nonKebabCaseParams);
        final var methodInfo = getMethodInfo(annotationInstance);
        final var declaringClassName = getDeclaringClassName(methodInfo);
        throw new MessageHandlerValidationException(
                String.format("Parameter(s) \"%s\" of method %s in class %s is not formatted in kebab case.",
                        nonKebabCaseParamsString, methodInfo.name(), declaringClassName));
    }

    private void validateMethodParamCount(final AnnotationInstance annotationInstance) {
        final var paramCount = validationRules.paramCount();
        if (paramCount == null) {
            return;
        }

        final var methodInfo = getMethodInfo(annotationInstance);
        if (methodInfo.parameters().size() != paramCount) {
            throw new MessageHandlerValidationException(
                    String.format(
                            "MessageHandler annotated method %s::%s must declare exactly %s parameters that represents the event.",
                            methodInfo.declaringClass(),
                            methodInfo.name(),
                            paramCount));
        }
    }

    private void validateMethodReturnType(final AnnotationInstance annotationInstance) {
        final var methodInfo = getMethodInfo(annotationInstance);
        final var returnType = methodInfo.returnType();

        if (returnType.kind() == Type.Kind.VOID) {
            return;
        }

        if (returnType.kind() != Type.Kind.CLASS) {
            throw new MessageHandlerValidationException(
                    String.format(
                            "MessageHandler annotated method %s::%s must either have a class or void return type.",
                            methodInfo.declaringClass(),
                            methodInfo.name()));
        }

        final var classInfo = index.getClassByName(returnType.name());
        final var annotation = classInfo.classAnnotation(DOT_NAME_PRODUCED_EVENT);
        if (annotation == null) {
            throw new MessageHandlerValidationException(
                    String.format(
                            "MessageHandler annotated method %s::%s must either have a return object class annotated with @%s annotation or have a void return type.",
                            methodInfo.declaringClass(),
                            methodInfo.name(),
                            DOT_NAME_PRODUCED_EVENT.withoutPackagePrefix()));
        }

        classAnnotationValidator.validate(annotation);
    }

    private void validateMethodParamExternalDependency(final AnnotationInstance annotationInstance) {
        final var methodInfo = annotationInstance.target().asMethod();
        final var parameterType = methodInfo.parameters().get(0);
        final var classByName = index.getClassByName(parameterType.name());

        if (classByName == null) {
            throw new MessageHandlerValidationException(
                    String.format(
                            "MessageHandler annotated method %s::%s can not have external dependency classes as parameters.",
                            methodInfo.declaringClass(),
                            methodInfo.name()));
        }

    }

    private MethodInfo getMethodInfo(final AnnotationInstance annotationInstance) {
        return annotationInstance.target().asMethod();
    }

    private DotName getDeclaringClassName(final MethodInfo methodInfo) {
        return methodInfo.declaringClass().name();
    }
}
