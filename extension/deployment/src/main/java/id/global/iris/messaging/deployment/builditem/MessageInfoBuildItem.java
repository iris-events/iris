package id.global.iris.messaging.deployment.builditem;

import org.jboss.jandex.ClassInfo;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.Scope;
import io.quarkus.builder.item.MultiBuildItem;

public final class MessageInfoBuildItem extends MultiBuildItem {
    private final ClassInfo annotatedClassInfo;
    private final ExchangeType exchangeType;
    private final String name;
    private final String routingKey;
    private final Scope scope;

    public MessageInfoBuildItem(
            ClassInfo annotatedClassInfo,
            ExchangeType exchangeType,
            String name,
            String routingKey,
            Scope scope) {
        this.annotatedClassInfo = annotatedClassInfo;
        this.exchangeType = exchangeType;
        this.name = name;
        this.routingKey = routingKey;
        this.scope = scope;
    }

    public ClassInfo getAnnotatedClassInfo() {
        return annotatedClassInfo;
    }

    public ExchangeType getExchangeType() {
        return exchangeType;
    }

    public String getName() {
        return name;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public Scope getScope() {
        return scope;
    }
}
