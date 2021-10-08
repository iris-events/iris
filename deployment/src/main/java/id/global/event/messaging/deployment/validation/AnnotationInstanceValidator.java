package id.global.event.messaging.deployment.validation;

import java.util.regex.Pattern;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import id.global.event.messaging.deployment.MessageHandlerValidationException;

public class AnnotationInstanceValidator {
    private static final String KEBAB_CASE_PATTERN = "^([a-z][a-z0-9]*)(-[a-z0-9]+)*$";

    protected final IndexView index;
    protected final ValidationRules validationRules;

    public AnnotationInstanceValidator(final IndexView index, final ValidationRules validationRules) {
        this.index = index;
        this.validationRules = validationRules;
    }

    public void validate(final AnnotationInstance annotationInstance) {
        validateMethodParamCount(annotationInstance);
        validateParamsExist(annotationInstance);
        validateParamsAreKebabCase(annotationInstance);

        if (!validationRules.allowExternalDependencyParams()) {
            validateMethodParamExternalDependency(annotationInstance);
        }
    }

    public void validateMethodParamCount(final AnnotationInstance annotationInstance) {
        final var paramCount = validationRules.paramCount();
        if (paramCount == null) {
            return;
        }
        final var methodInfo = annotationInstance.target().asMethod();
        if (methodInfo.parameters().size() != paramCount) {
            throw new MessageHandlerValidationException(
                    String.format(
                            "MessageHandler annotated method %s::%s must declare exactly %s parameters that represents the event",
                            methodInfo.declaringClass(),
                            methodInfo.name(),
                            paramCount));
        }
    }

    public void validateMethodParamExternalDependency(final AnnotationInstance annotationInstance) {
        final var methodInfo = annotationInstance.target().asMethod();
        final var parameterType = methodInfo.parameters().get(0);
        final var classByName = index.getClassByName(parameterType.name());

        if (classByName == null) {
            throw new MessageHandlerValidationException(
                    String.format(
                            "MessageHandler annotated method %s::%s can not have external dependency classes as parameters",
                            methodInfo.declaringClass(),
                            methodInfo.name()));
        }

    }

    public void validateParamsAreKebabCase(final AnnotationInstance annotationInstance) {
        if (validationRules.kebabCaseOnlyParams() == null) {
            return;
        }
        for (String param : validationRules.kebabCaseOnlyParams()) {
            validateParamIsKebabCase(param, annotationInstance);
        }
    }

    public void validateParamsExist(final AnnotationInstance annotationInstance) {
        final var params = validationRules.requiredParams();
        if (params == null) {
            return;
        }
        for (String param : params) {
            validateParamExists(param, annotationInstance);
        }
    }

    public void validateParamIsKebabCase(final String param, final AnnotationInstance annotationInstance) {
        final var annotationValue = annotationInstance.value(param);
        final var matchesKebabCase = Pattern.compile(KEBAB_CASE_PATTERN).matcher(annotationValue.asString())
                .matches();

        if (!matchesKebabCase) {
            final var methodInfo = annotationInstance.target().asMethod();
            final var className = methodInfo.declaringClass().name().toString();
            final var methodName = methodInfo.name();
            throw new MessageHandlerValidationException(
                    String.format("Parameter \"%s\" on method %s in class %s is not formatted in kebab case.",
                            annotationValue.name(), methodName, className));
        }
    }

    private void validateParamExists(final String param, final AnnotationInstance annotationInstance) {
        if (annotationInstance.value(param) == null) {
            final var methodInfo = annotationInstance.target().asMethod();
            throw new MessageHandlerValidationException(
                    String.format("Parameter \"%s\" missing on MessageHandler annotation on %s::%s",
                            param,
                            methodInfo.declaringClass(),
                            methodInfo.name()));
        }
    }
}
