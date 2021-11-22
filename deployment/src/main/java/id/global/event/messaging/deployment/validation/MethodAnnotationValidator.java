package id.global.event.messaging.deployment.validation;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import id.global.amqp.parsers.ExchangeTypeParser;
import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.Message;
import id.global.event.messaging.deployment.MessageHandlerValidationException;
import id.global.event.messaging.deployment.constants.AnnotationInstanceParams;
import id.global.event.messaging.deployment.scanner.MessageHandlerScanner;

class MethodAnnotationValidator extends AbstractAnnotationInstanceValidator {

    private final DotName DOT_NAME_PRODUCED_EVENT = DotName.createSimple(Message.class.getCanonicalName());

    private final IndexView index;
    private final ClassAnnotationValidator classAnnotationValidator;

    public MethodAnnotationValidator(final IndexView index, final ClassAnnotationValidator classAnnotationValidator) {
        super();
        this.index = index;
        this.classAnnotationValidator = classAnnotationValidator;
    }

    @Override
    public void validate(final AnnotationInstance annotationInstance, IndexView index) {
        super.validate(annotationInstance, index);
        validateBindingKeysValidity(annotationInstance);
        validateMethodReturnType(annotationInstance);
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

    @Override
    protected ExchangeType getExchangeType(final AnnotationInstance annotationInstance, IndexView index) {
        final var methodInfo = getMethodInfo(annotationInstance);
        final var messageAnnotation = MessageHandlerScanner.getMessageAnnotation(methodInfo.parameters(), this.index);

        return ExchangeTypeParser.getFromAnnotationInstance(messageAnnotation, index);
    }

    private void validateBindingKeysValidity(final AnnotationInstance annotationInstance) {
        final var annotationValue = annotationInstance.value(AnnotationInstanceParams.BINDING_KEYS_PARAM);
        if (annotationValue == null) {
            return;
        }

        final var exchangeType = getExchangeType(annotationInstance, index);

        var patternString = KEBAB_CASE_PATTERN;
        if (exchangeType.equals(ExchangeType.TOPIC)) {
            patternString = TOPIC_PATTERN;
        }

        final var pattern = Pattern.compile(patternString);
        final var bindingKeys = Arrays.asList(annotationValue.asStringArray());
        bindingKeys.stream()
                .filter(bindingKey -> !pattern.matcher(bindingKey).matches())
                .findAny()
                .ifPresent(bindingKey -> {
                    final var methodInfo = getMethodInfo(annotationInstance);
                    final var declaringClassName = getDeclaringClassName(methodInfo);
                    throw new MessageHandlerValidationException(
                            String.format(
                                    "bindingKeys \"%s\" on message handler annotation in class %s does not conform to the correct format. "
                                            + "For TOPIC exchange type can contain only lowercase alphanumerical characters, dots and wildcards [*,#]. "
                                            + "For all others it should be formatted in kebab case.",
                                    bindingKey,
                                    declaringClassName));
                });
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

        classAnnotationValidator.validate(annotation, index);
    }

    private MethodInfo getMethodInfo(final AnnotationInstance annotationInstance) {
        return annotationInstance.target().asMethod();
    }

    private DotName getDeclaringClassName(final MethodInfo methodInfo) {
        return methodInfo.declaringClass().name();
    }
}
