package id.global.iris.messaging.deployment.builditem;

import id.global.iris.common.annotations.ExchangeType;
import id.global.iris.common.annotations.Scope;
import io.quarkus.builder.item.MultiBuildItem;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

public class MessageInfoBuildItem extends MultiBuildItem {
    // this might be better as Type, it's the same in MessageHandlerInfoBuildItem
    private final Type annotatedClassType;
    private final ExchangeType exchangeType;
    private final String name;
    private final String routingKey;
    private final Scope scope;
    private final int ttl;
    private final String deadLetter;
    private final boolean persistent;

    public MessageInfoBuildItem(final Type annotatedClassType, final ExchangeType exchangeType, final String name,
            final String routingKey,
            final Scope scope, final int ttl, final String deadLetter, final boolean persistent) {
        this.annotatedClassType = annotatedClassType;
        this.exchangeType = exchangeType;
        this.name = name;
        this.routingKey = routingKey;
        this.scope = scope;
        this.ttl = ttl;
        this.deadLetter = deadLetter;
        this.persistent = persistent;
    }

    public Type getAnnotatedClassType() {
        return annotatedClassType;
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

    public int getTtl() {
        return ttl;
    }

    public String getDeadLetter() {
        return deadLetter;
    }

    public boolean isPersistent() {
        return persistent;
    }

    // we'll also need additional fields for declaring exchange in case of a produced event
}
