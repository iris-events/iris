package id.global.event.messaging.deployment.validation;

import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import id.global.common.annotations.amqp.ExchangeType;
import id.global.event.messaging.deployment.MessageHandlerValidationException;

abstract class AbstractAnnotationInstanceValidator {
    static final String KEBAB_CASE_PATTERN = "^([a-z][a-z0-9]*)(-[a-z0-9]+)*$";
    static final String TOPIC_PATTERN = "^([*#]|[a-z0-9-]+)([.]([*#]|[a-z0-9-]+))*$";

    public AbstractAnnotationInstanceValidator() {
    }

    protected abstract MessageHandlerValidationException createNonKebabCaseParamsFoundException(
            final AnnotationInstance annotationInstance, final Set<String> nonKebabCaseParams);

    protected abstract ExchangeType getExchangeType(AnnotationInstance annotationInstance, IndexView index);

    public abstract void validate(final AnnotationInstance annotationInstance, IndexView index);
}
