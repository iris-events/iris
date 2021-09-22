package id.global.event.messaging.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Type;

import id.global.asyncapi.spec.enums.ExchangeType;
import io.quarkus.builder.item.MultiBuildItem;

public final class MessageHandlerInfoBuildItem extends MultiBuildItem {
    private final ClassInfo declaringClass;
    private final Type parameterType;
    private final String exchange;
    private final String queue;
    private final String methodName;
    private final ExchangeType exchangeType;
    private final String[] bindingKeys;

    public MessageHandlerInfoBuildItem(
            final ClassInfo declaringClass,
            final Type parameterType,
            final String methodName,
            final String queue,
            final String exchange,
            final String[] bindingKeys,
            final ExchangeType exchangeType) {
        this.declaringClass = declaringClass;
        this.parameterType = parameterType;
        this.exchange = exchange;
        this.queue = queue;
        this.methodName = methodName;
        this.bindingKeys = bindingKeys;
        this.exchangeType = exchangeType;
    }

    public ClassInfo getDeclaringClass() {
        return declaringClass;
    }

    public Type getParameterType() {
        return parameterType;
    }

    public String getExchange() {
        return exchange;
    }

    public String getQueue() {
        return queue;
    }

    public String getMethodName() {
        return methodName;
    }

    public String[] getBindingKeys() {
        return bindingKeys;
    }

    public ExchangeType getExchangeType() {
        return exchangeType;
    }

    @Override
    public String toString() {
        return "MessageHandlerInfoBuildItem{" +
                "declaringClass=" + declaringClass +
                ", parameterType=" + parameterType +
                ", exchange='" + exchange + '\'' +
                ", queue='" + queue + '\'' +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}
