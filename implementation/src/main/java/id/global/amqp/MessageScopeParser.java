package id.global.amqp;

import java.lang.reflect.Method;
import java.util.Objects;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import id.global.common.annotations.amqp.Message;
import id.global.common.annotations.amqp.Scope;

public class MessageScopeParser {
    private static final String MESSAGE_SCOPE_PARAM = "scope";

    public static Scope getFromAnnotationClass(Message messageAnnotation) throws NoSuchMethodException {
        final var scope = messageAnnotation.scope();
        if (!Objects.isNull(scope)) {
            return scope;
        }
        Method method = messageAnnotation.annotationType().getMethod(MESSAGE_SCOPE_PARAM);
        return (Scope) method.getDefaultValue();
    }

    public static Scope getFromAnnotationInstance(AnnotationInstance messageAnnotation, IndexView index) {
        return Scope.valueOf(messageAnnotation.valueWithDefault(index, MESSAGE_SCOPE_PARAM).asString());
    }
}
