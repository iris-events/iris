package id.global.event.messaging.deployment.validation;

import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import id.global.amqp.parsers.ExchangeTypeParser;
import id.global.common.annotations.amqp.ExchangeType;
import id.global.event.messaging.deployment.MessageHandlerValidationException;

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
    protected ExchangeType getExchangeType(AnnotationInstance annotationInstance, IndexView index) {
        return ExchangeTypeParser.getFromAnnotationInstance(annotationInstance, index);
    }

    private DotName getTargetClassName(final AnnotationInstance annotationInstance) {
        return annotationInstance.target().asClass().name();
    }
}
