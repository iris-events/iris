package id.global.amqp;

import java.lang.reflect.Method;
import java.util.Objects;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

import id.global.common.annotations.amqp.ExchangeType;
import id.global.common.annotations.amqp.Message;

public class ExchangeTypeParser {
    private static final String MESSAGE_EXCHANGE_TYPE_PARAM = "exchangeType";

    public static ExchangeType getFromAnnotationClass(Message messageAnnotation) throws NoSuchMethodException {
        final var exchangeType = messageAnnotation.exchangeType();
        if (!Objects.isNull(exchangeType)) {
            return exchangeType;
        }
        Method exchangeTypeMethod = messageAnnotation.annotationType().getMethod(MESSAGE_EXCHANGE_TYPE_PARAM);
        return (ExchangeType) exchangeTypeMethod.getDefaultValue();
    }

    public static ExchangeType getFromAnnotationInstance(AnnotationInstance messageAnnotation, IndexView index) {
        return ExchangeType.fromType(messageAnnotation.valueWithDefault(index, MESSAGE_EXCHANGE_TYPE_PARAM).asString());
    }
}
