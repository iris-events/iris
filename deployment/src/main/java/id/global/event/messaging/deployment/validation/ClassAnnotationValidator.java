package id.global.event.messaging.deployment.validation;

import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import id.global.common.annotations.amqp.ExchangeType;
import id.global.event.messaging.deployment.MessageHandlerValidationException;
import id.global.event.messaging.deployment.scanner.MessageHandlerScanner;

class ClassAnnotationValidator extends AbstractAnnotationInstanceValidator {

    public ClassAnnotationValidator() {
        super();
    }

    @Override
    protected MessageHandlerValidationException createNonKebabCaseParamsFoundException(
            final AnnotationInstance annotationInstance, final Set<String> nonKebabCaseParams) {

        final var nonKebabCaseParamsString = String.join(", ", nonKebabCaseParams);
        throw new MessageHandlerValidationException(
                String.format("Parameter(s) \"%s\" of annotation %s on class %s is not formatted in kebab case.",
                        nonKebabCaseParamsString, annotationInstance.name(), getTargetClassName(annotationInstance)));
    }

    @Override
    protected ExchangeType getExchangeType(AnnotationInstance annotationInstance) {
        return MessageHandlerScanner.getExchangeType(annotationInstance);
    }

    private DotName getTargetClassName(final AnnotationInstance annotationInstance) {
        return annotationInstance.target().asClass().name();
    }
}
