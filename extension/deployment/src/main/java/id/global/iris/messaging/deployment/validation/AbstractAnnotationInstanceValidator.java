package id.global.iris.messaging.deployment.validation;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import id.global.common.iris.annotations.ExchangeType;
import id.global.iris.messaging.deployment.MessageHandlerValidationException;

abstract class AbstractAnnotationInstanceValidator {
    protected static final List<String> RESERVED_NAME_EXCLUSIONS = List.of("iris-subscription", "iris-manager");
    protected static final Pattern KEBAB_CASE_PATTERN = Pattern.compile("^([a-z][a-z0-9]*)(-[a-z0-9]+)*$");
    protected static final Pattern TOPIC_PATTERN = Pattern.compile("^([*#]|[a-z0-9-]+)([.]([*#]|[a-z0-9-]+))*$");

    public AbstractAnnotationInstanceValidator() {
    }

    protected abstract MessageHandlerValidationException createNonKebabCaseParamsFoundException(
            final AnnotationInstance annotationInstance, final Set<String> nonKebabCaseParams);

    protected abstract ExchangeType getExchangeType(AnnotationInstance annotationInstance, IndexView index);

    public abstract void validate(final AnnotationInstance annotationInstance, IndexView index);
}
