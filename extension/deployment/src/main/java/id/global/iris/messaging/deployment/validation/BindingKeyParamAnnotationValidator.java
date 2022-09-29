package id.global.iris.messaging.deployment.validation;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.messaging.deployment.MessageHandlerValidationException;
import id.global.iris.messaging.deployment.constants.AnnotationInstanceParams;
import id.global.iris.messaging.deployment.scanner.ScannerUtils;
import id.global.iris.parsers.ExchangeTypeParser;

public class BindingKeyParamAnnotationValidator implements AnnotationInstanceValidator {

    private final IndexView index;

    public BindingKeyParamAnnotationValidator(IndexView index) {
        this.index = index;
    }

    @Override
    public void validate(AnnotationInstance annotationInstance) {
        final var annotationValue = annotationInstance.value(AnnotationInstanceParams.BINDING_KEYS_PARAM);
        if (annotationValue == null) {
            return;
        }

        final var exchangeType = getExchangeType(annotationInstance, index);

        final var pattern = getPattern(exchangeType);

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

    private Pattern getPattern(final ExchangeType exchangeType) {
        return exchangeType.equals(ExchangeType.TOPIC) ? TOPIC_PATTERN : KEBAB_CASE_PATTERN;
    }

    private ExchangeType getExchangeType(final AnnotationInstance annotationInstance, IndexView index) {
        final var methodInfo = getMethodInfo(annotationInstance);
        final var messageAnnotation = ScannerUtils.getMessageAnnotation(methodInfo, this.index);

        return ExchangeTypeParser.getFromAnnotationInstance(messageAnnotation, index);
    }

    private MethodInfo getMethodInfo(final AnnotationInstance annotationInstance) {
        return annotationInstance.target().asMethod();
    }

    private DotName getDeclaringClassName(final MethodInfo methodInfo) {
        return methodInfo.declaringClass().name();
    }

}
