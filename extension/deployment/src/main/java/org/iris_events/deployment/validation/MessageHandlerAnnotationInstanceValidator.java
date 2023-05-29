package org.iris_events.deployment.validation;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

public class MessageHandlerAnnotationInstanceValidator implements AnnotationInstanceValidator {

    private final List<AnnotationInstanceValidator> validators;

    public MessageHandlerAnnotationInstanceValidator(IndexView indexView, String serviceName) {
        final var messageAnnotationValidator = new MessageAnnotationValidator(serviceName, indexView);
        final var methodReturnTypeAnnotationValidator = new MethodReturnTypeAnnotationValidator(indexView,
                messageAnnotationValidator);

        final var methodParameterTypeAnnotationValidator = new MethodParameterTypeAnnotationValidator(indexView,
                List.of(messageAnnotationValidator));
        final var bindingKeyParamAnnotationValidator = new BindingKeyParamAnnotationValidator(indexView);

        final var perInstanceParamAnnotationValidator = new PerInstanceParamAnnotationValidator(indexView);
        this.validators = List.of(methodParameterTypeAnnotationValidator,
                methodReturnTypeAnnotationValidator, bindingKeyParamAnnotationValidator, perInstanceParamAnnotationValidator);
    }

    @Override
    public void validate(AnnotationInstance annotationInstance) {
        validators.forEach(validator -> validator.validate(annotationInstance));
    }
}
