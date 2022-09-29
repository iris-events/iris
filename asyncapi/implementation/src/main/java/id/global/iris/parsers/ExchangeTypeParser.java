package id.global.iris.parsers;

import java.lang.reflect.Method;
import java.util.Objects;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import id.global.iris.IrisAnnotationRuntimeException;
import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.Message;

public class ExchangeTypeParser {
    private static final String MESSAGE_EXCHANGE_TYPE_PARAM = "exchangeType";

    public static ExchangeType getFromAnnotationClass(Message messageAnnotation) {
        final var exchangeType = messageAnnotation.exchangeType();
        if (Objects.nonNull(exchangeType)) {
            return exchangeType;
        }

        try {
            Method exchangeTypeMethod = messageAnnotation.annotationType().getMethod(MESSAGE_EXCHANGE_TYPE_PARAM);
            return (ExchangeType) exchangeTypeMethod.getDefaultValue();
        } catch (NoSuchMethodException e) {
            throw new IrisAnnotationRuntimeException(
                    String.format("Malformed %s annotation. Does not contain %s parameter default",
                            Message.class.getName(),
                            MESSAGE_EXCHANGE_TYPE_PARAM));
        }

    }

    public static ExchangeType getFromAnnotationInstance(AnnotationInstance messageAnnotation, IndexView index) {
        return ExchangeType.fromType(messageAnnotation.valueWithDefault(index, MESSAGE_EXCHANGE_TYPE_PARAM).asString());
    }
}
