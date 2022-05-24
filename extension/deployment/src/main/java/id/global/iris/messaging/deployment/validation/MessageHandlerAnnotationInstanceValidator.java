package id.global.iris.messaging.deployment.validation;

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

        this.validators = List.of(methodParameterTypeAnnotationValidator,
                methodReturnTypeAnnotationValidator, bindingKeyParamAnnotationValidator);
    }

    @Override
    public void validate(AnnotationInstance annotationInstance) {
        validators.forEach(validator -> validator.validate(annotationInstance));
    }
}
