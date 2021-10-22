package id.global.event.messaging.deployment.validation;

import org.jboss.jandex.DotName;

public record ClassAnnotationDetails(DotName annotationClassName, DotName eventClassName) {
}
