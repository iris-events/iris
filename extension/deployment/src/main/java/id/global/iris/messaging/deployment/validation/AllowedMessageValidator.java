package id.global.iris.messaging.deployment.validation;

import java.util.List;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import id.global.iris.messaging.deployment.MessageHandlerValidationException;

public class AllowedMessageValidator implements AnnotationInstanceValidator {

    private final List<DotName> allowedDotNames;

    public AllowedMessageValidator(List<DotName> allowedDotNames) {
        this.allowedDotNames = allowedDotNames;
    }

    @Override
    public void validate(AnnotationInstance annotationInstance) {
        final var messageDotName = annotationInstance.target().asClass()
                .name();

        final var noneMatch = allowedDotNames.stream().noneMatch(dotName -> dotName.equals(messageDotName));
        if (noneMatch) {
            final var dotNames = allowedDotNames.stream().map(DotName::toString)
                    .collect(Collectors.joining(", ", "[", "]"));
            throw new MessageHandlerValidationException(
                    "One of " + dotNames + " classes should be used as a parameter for the message handler.");
        }
    }
}
