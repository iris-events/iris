package id.global.iris.messaging.deployment.validation;

import java.util.regex.Pattern;

import org.jboss.jandex.AnnotationInstance;

public interface AnnotationInstanceValidator {

    Pattern KEBAB_CASE_PATTERN = Pattern.compile("^([a-z0-9-]*)(/)?(-?[a-z0-9]+)*$");
    Pattern TOPIC_PATTERN = Pattern.compile("^([*#]|[a-z0-9-]+)([.]([*#]|[a-z0-9-]+))*$");

    void validate(AnnotationInstance annotationInstance);
}
