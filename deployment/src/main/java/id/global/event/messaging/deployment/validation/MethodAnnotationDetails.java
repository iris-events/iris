package id.global.event.messaging.deployment.validation;

import org.jboss.jandex.DotName;

public record MethodAnnotationDetails(String methodName, DotName methodDeclarationClassName) {
}
